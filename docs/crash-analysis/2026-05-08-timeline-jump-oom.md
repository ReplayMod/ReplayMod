# タイムラインジャンプ時の `OutOfMemoryError` 解析レポート

- **発生日**: 2026-05-08 06:32:19
- **対象 Minecraft バージョン**: 1.21.4 (Fabric)
- **ReplayMod ビルド**: `1.21.4-2.6.26-1-g50ef322-dirty`
- **元クラッシュログ**: [`logs/clash1.txt`](../../logs/clash1.txt)
- **修正対象ブランチ**: `claude/fix-mod-crash-xWvTo`

---

## 1. 症状

ユーザーがリプレイ画面のタイムラインをマウスでクリックして大きく時刻ジャンプを行った際、
レンダースレッドで `java.lang.OutOfMemoryError: Java heap space` が発生してゲーム本体ごと
クラッシュした。割り当て上限は **24 GB** (`-Xmx24000m`)。

クラッシュ時点のスタックトレース (要約):

```
java.lang.OutOfMemoryError: Java heap space
  at java.util.ArrayList.add(ArrayList.java:496)
  at net.minecraft.world.World.addBlockEntityTicker(World.java:471)             [class_1937.method_31594]
  at net.minecraft.world.chunk.WorldChunk.<lambda>(WorldChunk.java:685)         [class_2818.method_31719]
  at it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap.compute(...)
  at net.minecraft.world.chunk.WorldChunk.updateGameEventListener(WorldChunk.java:677)  [class_2818.method_31723]
  at net.minecraft.world.chunk.WorldChunk.removeBlockEntity(WorldChunk.java:382)        [class_2818.method_12216]
  at net.minecraft.world.World.removeBlock(World.java:587)                              [class_1937.method_8438]
  at net.minecraft.block.PistonBlock.tryMove(PistonBlock.java:362)                      [class_2665.method_11481]
  at net.minecraft.block.PistonBlock.onSyncedBlockEvent(PistonBlock.java:244)           [class_2665.method_9592]
  at net.minecraft.world.World.addSyncedBlockEvent(World.java:773)                      [class_1937.method_8427]
  at net.minecraft.client.network.ClientPlayNetworkHandler.onBlockEvent(...)
  at net.minecraft.network.packet.s2c.play.BlockEventS2CPacket.apply(BlockEventS2CPacket.java:49)
  at net.minecraft.client.MinecraftClient.replayModExecuteTaskQueue(MinecraftClient.java:18595)
  at com.replaymod.replay.FullReplaySender.executeTaskQueue(FullReplaySender.java:1392)
  at com.replaymod.replay.FullReplaySender.sendPacketsTill(FullReplaySender.java:1295)
  at com.replaymod.replay.ReplayHandler.doJump(ReplayHandler.java:885)
  at com.replaymod.replay.gui.overlay.GuiReplayOverlay$7.run(GuiReplayOverlay.java:132)
  at de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline.onClick(AbstractGuiTimeline.java:198)
  at de.johni0702.minecraft.gui.element.advanced.AbstractGuiTimeline.mouseClick(AbstractGuiTimeline.java:281)
```

最も近い JVM フレームは `ArrayList.add` の `Arrays.copyOf` で、
`World.addBlockEntityTicker` (`method_31594`) が呼んだリストへの追加で
ヒープが枯渇している。

## 2. 根本原因

### 2.1 該当する Minecraft 側のデータ構造

Minecraft 1.17 以降、`World` は次の 2 本のリストでブロックエンティティの tick 対象を管理している。

| フィールド                         | 用途                                                  |
|------------------------------------|-------------------------------------------------------|
| `pendingBlockEntityTickers`        | これから tick 対象に追加したい新規 ticker のバッファ |
| `blockEntityTickers`               | 実際に毎 tick 走るアクティブな ticker                |

通常フローでは **毎 tick 一度** だけ呼ばれる `World.tickBlockEntities()` が、
1 行目で `blockEntityTickers.addAll(pendingBlockEntityTickers)` を実行し、続けて
`pendingBlockEntityTickers.clear()` で空にする。同じメソッド内で `removeIf(isRemoved)`
により死んだ ticker も毎 tick 取り除かれる。

つまり `pendingBlockEntityTickers` は「世界が 1 回でも tick されれば必ず空になる」
という暗黙の不変条件のもとで設計されている。

### 2.2 ReplayMod の同期パケット処理パス

時刻ジャンプの実体である [`ReplayHandler.doJump`](../../src/main/java/com/replaymod/replay/ReplayHandler.java) は、
ジャンプ幅が大きい場合に [`FullReplaySender.sendPacketsTill`](../../src/main/java/com/replaymod/replay/FullReplaySender.java) を介して
リプレイファイル中の何万〜何百万パケットをまとめて投入する。

主要ループは概ね次の通り:

```java
// FullReplaySender.sendPacketsTill (sync mode)
syncSender.submit(() -> doSendPacketsTill(timestamp));   // 別スレッドが channel.fireChannelRead
while (!doneSending.get()) {
    executeTaskQueue();                                  // ← メインスレッドでパケット適用
    Thread.sleep(0, 100_000);
}
executeTaskQueue();                                      // 最後に残ったタスクを排出
```

`executeTaskQueue` は `MinecraftClient.runTasks()` を呼ぶだけで、**ワールドの tick は行わない**。
さらに上位の `ReplayHandler.doJump` の `do { sendPacketsTill(...) } while (mc.player == null || ...)` ループ
( `ReplayHandler.java:901-908` ) も、すべて終わった後に `mc.tick()` を 1 度呼ぶのみで、
途中ではワールドを tick しない。

### 2.3 何が起きるか

その結果として、ジャンプ中は以下の状態が長時間続く。

1. パケット (`BlockEventS2CPacket`、ピストンの伸縮、ブロック更新、チャンクロード等) が
   次々と適用される。
2. それぞれが `World.addBlockEntityTicker` を呼び、`pendingBlockEntityTickers` に
   ticker を追加する。
3. 同じブロック (例: 高頻度に伸縮するピストン、リドストーン制御の頻繁な block entity)
   が何度もリセットされ、その度に新しい ticker が `pending` に積まれる一方、
   古い ticker は別ルートで `isRemoved` 化される。
4. **しかし `pendingBlockEntityTickers` のサイズだけは tick が来るまで誰も縮めない。**
5. 数十秒〜数分のジャンプで pending リストが数千万〜数億件まで膨張し、
   `ArrayList.grow` の `Arrays.copyOf` がヒープを枯渇させる (24 GB でも足りない)。

クラッシュレポートが「単一のピストンの BlockEvent」上で OOM になっているのは、
リプレイのその瞬間にリストの再確保がたまたま起きただけで、ピストン自体は
最後の藁にすぎない。

### 2.4 影響範囲

- **再現条件**: FullReplaySender (= 通常モード / Quick Mode 未使用 or 大ジャンプ) で、
  数分以上を一気にスキップするユースケース全般。
- **トリガー導線**:
  - タイムラインクリック → `GuiReplayOverlay$7.run` → `doJump`
  - GUI Render Queue (`GuiRenderQueue.java:241`) でリプレイの世界ロード待ち
  - ビデオレンダリング前のシーク (`VideoRenderer.java:262`) は各フレームで `tick()` を
    呼ぶため通常は安全だが、シークそのものを大きく行う場合は同じ問題に晒される
    (今回の修正で同じ `executeTaskQueue` 経由のため自動的に救済される)
- **MC バージョン**: 1.17 (21w19a) 以降。`pendingBlockEntityTickers` 自体が
  この時点で導入されたため、それ以前は対象外。

---

## 3. 修正方針

`FullReplaySender.executeTaskQueue` の末尾で `pendingBlockEntityTickers` を
**手動で `blockEntityTickers` に合流させ、`isRemoved` のものを除去** する。
ロジックは `World.tickBlockEntities` のドレイン部分そのものを抜き出した形で、
ticker の `tick()` 自体は呼ばない (= 副作用なし、ワールド時刻も進めない)。

しきい値 1024 を設けて、毎フラッシュごとに走らせず通常動作のオーバーヘッドを抑える。
これにより:

- pending リストは最大でも 1024 + 1 バッチ分しか溜まらない
- active リストも同タイミングで `removeIf(isRemoved)` され、肥大化しない
- 通常 (アジャストなしの) tick が来たときに同じドレインが再び走るが、空リストへの
  no-op になるだけで害はない

スレッド安全性: `executeTaskQueue` も `World.tickBlockEntities` も Minecraft の
レンダースレッド (= メインスレッド) でしか実行されない。pending/active リスト自体も
そのスレッドからのみ触られるため、ロックは不要。

### 3.1 変更ファイル

| ファイル | 変更内容 |
|---|---|
| `src/main/java/com/replaymod/replay/mixin/WorldAccessor.java` | 新規。`World#pendingBlockEntityTickers` と `blockEntityTickers` への `@Accessor`。MC>=11700 限定。 |
| `src/main/resources/mixins.replay.replaymod.json` | `WorldAccessor` を 1.17+ 限定で登録。 |
| `src/main/java/com/replaymod/replay/FullReplaySender.java` | `executeTaskQueue()` 末尾で `drainPendingBlockEntityTickers()` を呼ぶ。1.17+ で pending を回収・死亡 ticker を削除。 |

### 3.2 修正後の動作

- 短いジャンプ: pending サイズが 1024 未満なら何もしない (今までと同じパス)。
- 長いジャンプ / リプレイのワールドロード待ち: 1024 件溜まる毎に合流＋掃除。
  メモリ使用量はジャンプ時間に対して O(1) で安定する。

---

## 4. 副次的な調査と再発防止

似た「同期パケット処理ループ中にワールド tick が起きない」パターンを探したが、
すべて最終的に `FullReplaySender.executeTaskQueue` を呼ぶため今回の修正で同時に救われる。

| 呼び出し元 | パス | 状態 |
|---|---|---|
| `ReplayHandler.doJump` (大ジャンプ) | `sendPacketsTill` (n回) → `executeTaskQueue` | ✅ 修正対象に含まれる |
| `ReplayHandler.doJump` (paused 小ジャンプ) | 同上 | ✅ 同上 |
| `ReplayHandler.restartedReplay` 周辺 (`replayHandler.java:528-532`) | `sendPacketsTill` | ✅ 同上 |
| `VideoRenderer` 初期シーク (`VideoRenderer.java:262`) | `sendPacketsTill` + 各フレーム `tick()` | ✅ 同上 (tick も走るため二重に安全) |
| `GuiRenderQueue.processNextReplay` (`GuiRenderQueue.java:241`) | `sendPacketsTill` | ✅ 同上 |
| `QuickReplaySender.sendPacketsTill` | `replay.seek()` (replaystudio) — 通常 `PreTickCallback` 経由で動くため毎 tick ドレインされる | ⚪ 影響なし |
| `VideoRenderer.executeTaskQueue` (リソースリロード待ち) | パケット適用ではなく overlay/reload 待機なので tickers は積まれない | ⚪ 影響なし |

`FullReplaySender` 一箇所を直すだけで、リプレイのワールド更新を伴う同期パスは
全て対策される構造になっていることを確認した。

---

## 5. 再現と確認手順 (推奨)

1. 1.21.4 + Fabric + 上記 mod 構成のテスト環境を用意。
2. 大量のピストン or レッドストーン稼働 + 多人数のリプレイ (今回のクラッシュ環境と
   同じ「the_nether で稼働する装置を含むサーバ」でも良い) を録画。
3. リプレイを開き、タイムラインを **数分先** までクリックジャンプ。
4. 修正前: 数十秒〜数分でレンダースレッドが応答しなくなり OOM クラッシュ。
   修正後: メモリ使用量がプラトーになり、ジャンプは完了する。
5. ジャンプ後にピストンや高頻度更新ブロックが正しく描画 / 動作することを確認。

---

## 6. 関連 / 余談

- `OPTIMIZATION_LOG.md` のメモリ削減方針 (Goal #2) と同方向の改善であり、
  巨大リプレイで複数連続ジャンプを行うワークフローでも体感メモリが安定する。
- jGui 側 (`AbstractGuiTimeline.mouseClick`) は呼び出しトリガでしかなく、
  jGui 自体のバグではない。サブモジュール `jGui/` は本リポジトリで未チェックアウト
  だが触る必要なし。
- 本件は `OutOfMemoryError` というクライアント側の Java 制約に起因しており、
  `BAD_PACKETS` (`FullReplaySender.java:194-` でフィルタするやつ) を増やして
  対症療法することは推奨しない (再生品質が落ちるため)。
