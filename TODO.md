速度を8倍にしてもなぜか等倍になる不具合を修正
→ 修正済み: `Mixin_LiftReplayTickRateCap` でリプレイ中のみ Math.min(10, ticks) のキャップを 200 まで緩和。
→ 低FPS時に tick 消化が頭打ちになって実効速度が 1x に落ちていた症状が解消する。

処理関連が追いついていない場合に等倍になる可能性？
→ 上記キャップ緩和で大半が解消する。残りはレンダースレッド負荷自体の問題。

クラッシュしないギリギリまで性能を使うように
→ tick rate キャップ 200, 共有 ForkJoinPool/IO_POOL を継続利用 (PARALLEL-01)。

イーターの後半 30 分時点とかに行く時のお待ちくださいが長時間表示される問題
→ doJump の large-jump branch で QuickReplaySender 初期化を裏でトリガするように変更。
→ 初回ジャンプ時はまだ重いが、以降のジャンプは QuickMode が温まっており O(log n) でシークできる。

読み込み中に GPU を使っていない問題
→ 読み込み (.mcpr 解凍 + パケットデコード + EntityPositionTracker 構築) は I/O と CPU bound であり GPU オフロードには適さない。
→ GPU は描画 (GPU-01 マルチスレッドメッシュ構築) とエンコード (GPU-02 NVENC/VAAPI ハードウェアエンコード) で既に活用済み。
→ ハイブリッド GPU 構成 (AMD iGPU + NVIDIA dGPU) で NVENC native パスが失敗するクラッシュは Mixin_LiftReplayTickRateCap とは別の経路で対処済み:
   * Java 側 (`NativeOpenGlEncoder.shouldUse`) で OpenGL renderer が NVIDIA でない場合は早期に FFmpeg fallback。
   * Native 側 (`replaymod_native_encoder.cpp`) で cuGLGetDevices が NVIDIA デバイスを返さない場合は明示的に失敗 (cuDeviceGet(0) フォールバックを廃止)。
   * Blackwell ドライバ向けに NVENC API ≥ 12 ヘッダで DLL を再ビルド。outputAUD など Blackwell ドライバが拒否する設定は除去。
→ Windows 設定の Graphics Performance で javaw.exe を「高パフォーマンス」(NVIDIA) に設定すると、native NVENC パスがそのまま使える。
