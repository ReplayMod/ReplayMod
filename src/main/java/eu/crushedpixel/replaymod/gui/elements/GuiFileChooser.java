package eu.crushedpixel.replaymod.gui.elements;

import eu.crushedpixel.replaymod.gui.elements.listeners.FileChooseListener;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GuiFileChooser extends GuiAdvancedButton {

    @Getter
    private File selectedFile;

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
        updateDisplayString();
    }

    protected String baseString;
    private boolean save = false;

    @Setter
    private String[] allowedExtensions = null;

    private List<FileChooseListener> listeners = new ArrayList<FileChooseListener>();

    public GuiFileChooser(int buttonId, int x, int y, String buttonText, File selectedFile, String[] allowedExtensions) {
        this(buttonId, x, y, buttonText, selectedFile, allowedExtensions, false);
    }

    public GuiFileChooser(int buttonId, int x, int y, String buttonText, File selectedFile, String[] allowedExtensions, boolean save) {
        super(buttonId, x, y, buttonText);
        this.selectedFile = selectedFile;
        this.save = save;

        this.baseString = buttonText;
        updateDisplayString();

        this.allowedExtensions = allowedExtensions;
    }

    public void addFileChooseListener(FileChooseListener listener) {
        this.listeners.add(listener);
    }

    protected void updateDisplayString() {
        this.displayString = baseString + (selectedFile == null ? "-" : selectedFile.getName());
    }

    @Override
    public void performAction() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch(Exception e) {
                    e.printStackTrace();
                }

                JFrame frame = new JFrame();

                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", allowedExtensions));

                if(selectedFile != null) {
                    fileChooser.setCurrentDirectory(selectedFile.getParentFile());
                    fileChooser.setSelectedFile(selectedFile);
                }

                fileChooser.setFileSelectionMode(save ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
                fileChooser.setVisible(true);
                fileChooser.grabFocus();
                fileChooser.showOpenDialog(frame);

                File file = fileChooser.getSelectedFile();

                if(file != null) {
                    selectedFile = file;

                    updateDisplayString();

                    for(FileChooseListener listener : listeners) {
                        listener.onFileChosen(selectedFile);
                    }
                }

                frame.dispose();
            }
        }, "replaymod-file-chooser").start();
    }
}
