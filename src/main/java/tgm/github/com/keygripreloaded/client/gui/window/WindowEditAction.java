package tgm.github.com.keygripreloaded.client.gui.window;

import com.google.common.base.Splitter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.registry.EntityEntry;

import java.util.Collections;

import me.ichun.mods.ichunutil.client.gui.Theme;
import me.ichun.mods.ichunutil.client.gui.window.IWorkspace;
import me.ichun.mods.ichunutil.client.gui.window.Window;
import me.ichun.mods.ichunutil.client.gui.window.element.Element;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementButton;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementCheckBox;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementNumberInput;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementSelector;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementTextInput;
import tgm.github.com.keygripreloaded.client.gui.GuiWorkspace;
import tgm.github.com.keygripreloaded.common.scene.Scene;
import tgm.github.com.keygripreloaded.common.scene.action.Action;

public class WindowEditAction extends Window
{
    public Action action;

    public WindowEditAction(IWorkspace parent, int x, int y, int w, int h, int minW, int minH)
    {
        super(parent, x, y, w, h, minW, minH, "window.editAction.title", true);

        Scene scene = ((GuiWorkspace)parent).getOpenScene();
        Action action = scene.actions.parallelStream()
                .filter(act -> act.identifier.equals(((GuiWorkspace)parent).timeline.timeline.selectedIdentifier))
                .findFirst().orElse(null);

        elements.add(new ElementTextInput(this, 10, 30, width - 20, 12, 1, "window.newAction.name", action != null ? action.name : ""));
        elements.add(new ElementTextInput(this, 10, 135, width - 20, 12, 2, "window.newAction.playerName", action != null && action.entityType != null && action.entityType.startsWith("player::") ? action.entityType.substring("player::".length()) : ""));
        ElementSelector selector = new ElementSelector(this, 10, 65, width - 20, 12, -2, "window.newAction.entityType", "EntityPlayer");

        for (EntityEntry o : net.minecraftforge.registries.GameData.getEntityRegistry())
        {
            Class<? extends Entity> clz = o.getEntityClass();
            if(EntityLivingBase.class.isAssignableFrom(clz) && Minecraft.getMinecraft().getRenderManager().getEntityClassRenderObject(clz) instanceof RenderLivingBase)
            {
                selector.choices.put(clz.getSimpleName(), clz);
            }
        }
        selector.choices.put(EntityPlayer.class.getSimpleName(), EntityPlayer.class);
        selector.selected = EntityPlayer.class.getSimpleName();

        if(action != null && action.entityType != null && selector.choices.containsKey(Splitter.on(".").splitToList(action.entityType).get(Splitter.on(".").splitToList(action.entityType).size() - 1)))
        {
            selector.selected = Splitter.on(".").splitToList(action.entityType).get(Splitter.on(".").splitToList(action.entityType).size() - 1);
        }

        elements.add(selector);
        elements.add(new ElementCheckBox(this, 11, 89, -1, false, 0, 0, "window.newAction.preCreate", (action != null && action.precreateEntity == 1)));
        elements.add(new ElementCheckBox(this, 11, 109, -2, false, 0, 0, "window.newAction.persist", (action != null && action.persistEntity == 1)));
        elements.add(new ElementNumberInput(this, 10, 170, 40, 12, 4, "window.editAction.actionPos", 1, false, 0, Integer.MAX_VALUE, action != null ? action.startKey : 0));
        elements.add(new ElementNumberInput(this, 10, 205, 160, 12, -1, "window.editAction.actionOffset", 3, true, -30000000, 30000000, action != null ? action.offsetPos[0] / (double)Scene.PRECISION : 0, action != null ? action.offsetPos[1] / (double)Scene.PRECISION : 0, action != null ? action.offsetPos[2] / (double)Scene.PRECISION : 0));

        elements.add(new ElementButton(this, width - 140, height - 30, 60, 16, 100, false, 1, 1, "element.button.ok"));
        elements.add(new ElementButton(this, width - 70, height - 30, 60, 16, 0, false, 1, 1, "element.button.cancel"));

        this.action = action;
    }

    @Override
    public void draw(int mouseX, int mouseY)
    {
        super.draw(mouseX, mouseY);
        if(!minimized)
        {
            workspace.getFontRenderer().drawString(I18n.format("window.newAction.name"), posX + 11, posY + 20, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.newAction.entityType"), posX + 11, posY + 55, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.newAction.preCreate"), posX + 23, posY + 90, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.newAction.persist"), posX + 23, posY + 110, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.newAction.playerName"), posX + 11, posY + 125, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.editAction.actionPos"), posX + 11, posY + 160, Theme.getAsHex(workspace.currentTheme.font), false);
            workspace.getFontRenderer().drawString(I18n.format("window.editAction.actionOffset"), posX + 11, posY + 195, Theme.getAsHex(workspace.currentTheme.font), false);
        }
    }

    @Override
    public void elementTriggered(Element element)
    {
        if(element.id == 0)
        {
            workspace.removeWindow(this, true);
        }
        if(element.id <= 0 || element.id == 3 || element.id == 4) return;

        String actName = "";
        String playerName = "";
        String clzName = "";
        boolean create = false;
        boolean persist = false;
        boolean isPlayer = false;
        int pos = 0;
        int[] startPos = new int[3];

        for (Element e : elements) {
            if (e instanceof ElementTextInput)
            {
                ElementTextInput text = (ElementTextInput) e;
                if (text.id == 1) {
                    actName = text.textField.getText();
                } else if (text.id == 2) {
                    playerName = text.textField.getText();
                }
            } else if (e instanceof ElementCheckBox) {
                if (e.id == -1) {
                    create = ((ElementCheckBox) e).toggledState;
                } else if (e.id == -2) {
                    persist = ((ElementCheckBox) e).toggledState;
                }
            } else if (e instanceof ElementSelector) {
                ElementSelector selector = (ElementSelector) e;
                Object obj = selector.choices.get(selector.selected);
                if (obj != null) {
                    clzName = ((Class) obj).getName();
                    if (EntityPlayer.class.isAssignableFrom((Class) obj)) {
                        isPlayer = true;
                        if (playerName.isEmpty()) {
                            playerName = Minecraft.getMinecraft().player.getName();
                        }
                    }
                }
            } else if (e instanceof ElementNumberInput) {
                ElementNumberInput nums = (ElementNumberInput) e;
                if (e.id == 4) {
                    pos = Integer.parseInt(nums.textFields.get(0).getText());
                } else if (e.id == -1) {
                    startPos = new int[]{(int) Math.round(Double.parseDouble(nums.textFields.get(0).getText()) * Scene.PRECISION), (int) Math.round(Double.parseDouble(nums.textFields.get(1).getText()) * Scene.PRECISION), (int) Math.round(Double.parseDouble(nums.textFields.get(2).getText()) * Scene.PRECISION)};
                }
            }
        }
        GuiWorkspace parent = (GuiWorkspace)workspace;
        Scene scene = parent.getOpenScene();
        if(actName.isEmpty())
        {
            actName = "NewAction" + (scene.actions.size() + 1);
        }
        if(action != null)
        {
            action.update(actName, !isPlayer ? clzName : "player::" + playerName, pos, null, create, persist);
            action.offsetPos = startPos;
        }
        Collections.sort(scene.actions);
        workspace.removeWindow(this, true);
    }
}
