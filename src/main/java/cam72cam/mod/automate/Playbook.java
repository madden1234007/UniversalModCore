package cam72cam.mod.automate;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Playbook extends Panel {
    private final File file;

    private final List<Action> actions = new ArrayList<>();
    private int actionIdx = 0;
    private boolean runStep = false;
    private boolean runAll = false;

    private final GridBagConstraints c;

    public Playbook(File file) throws IOException {
        super(new GridBagLayout());

        c = new GridBagConstraints();

        this.file = file;
        if (file.exists()) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                actions.add(Action.deserialize(line));
            }
        } else {
            actions.add(new GuiClickButton("Singleplayer"));
            actions.add(new GuiClickButton("Create New World"));
            actions.add(new GuiClickButton("Game Mode: Survival"));
            actions.add(new GuiClickButton("Game Mode: Hardcore"));
            actions.add(new GuiClickButton("More World Options..."));
            actions.add(new GuiClickButton("Generate Structures: ON"));
            actions.add(new GuiClickButton("World Type: Default"));
            actions.add(new GuiSetText("seed", "let it grow"));
            actions.add(new GuiClickButton("Create New World"));
            //actions.add(new GuiButtonClick("More World Options..."));
        }

        this.redraw();

        this.setVisible(true);
    }

    public void startover() {
        actionIdx = 0;
        runStep = false;
        runAll = true;
        redraw();
    }
    public void runStep() {
        runStep = true;
        redraw();
    }
    public void runAll() {
        runAll = true;
        redraw();
    }
    public void pause() {
        runStep = false;
        runAll = false;
        redraw();
    }
    public void stop() {
        runStep = false;
        runAll = false;
        actionIdx = 0;
        redraw();
    }
    public void save() throws IOException {
        saveAs(this.file);
    }

    public void saveAs(File file) throws IOException {
        Files.write(file.toPath(), actions.stream().map(Action::serialize).collect(Collectors.joining(System.lineSeparator())).getBytes());
    }

    public void tick() {
        if (runAll || runStep) {
            if (actionIdx == actions.size()) {
                stop();
                return;
            }

            if (!actions.isEmpty()) {
                if (actions.get(actionIdx).tick()) {
                    actionIdx++;
                    runStep = false;
                    redraw();
                }
            }
        }
    }

    private void redraw() {
        this.removeAll();
        for (int i = 0; i < actions.size(); i++) {
            Action action = actions.get(i);
            Label l = new Label(i + "");
            l.setAlignment(Label.CENTER);
            if (i == actionIdx) {
                l.setBackground(runStep || runAll ? Color.GREEN : Color.YELLOW);
            }
            l.setVisible(true);
            int finalI = i;
            l.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    if (actionIdx == finalI) {
                        runStep = true;
                    }
                    actionIdx = finalI;
                    redraw();
                    super.mouseClicked(mouseEvent);
                }
            });

            Panel sub = new Panel(new FlowLayout(FlowLayout.LEFT));
            action.renderEditor(sub);
            sub.setVisible(true);
            sub.revalidate();

            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 0;
            c.gridy = i;
            c.gridwidth = 1;
            this.add(l, c);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.gridy = i;
            c.gridwidth = 5;
            this.add(sub, c);
        }
        this.revalidate();
    }

    public void removeCurrentAction() {
        this.actions.remove(this.actionIdx);
        redraw();
    }

    public void appendAction(Action action) {
        this.actions.add(action);
        redraw();
    }

    public void insertAction(Action action) {
        this.actions.add(actionIdx+1, action);
        redraw();
    }
}