package tgm.github.com.keygripreloaded.client.gui.window;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;

import me.ichun.mods.ichunutil.client.gui.window.Window;
import me.ichun.mods.ichunutil.client.gui.window.WindowPopup;
import me.ichun.mods.ichunutil.client.gui.window.element.Element;
import me.ichun.mods.ichunutil.client.gui.window.element.ElementButtonTextured;
import me.ichun.mods.ichunutil.client.render.RendererHelper;
import tgm.github.com.keygripreloaded.client.gui.GuiWorkspace;
import tgm.github.com.keygripreloaded.client.gui.window.element.ElementTimeline;
import tgm.github.com.keygripreloaded.common.KeygripReloaded;
import tgm.github.com.keygripreloaded.common.packet.PacketStopScene;
import tgm.github.com.keygripreloaded.common.scene.Scene;
import tgm.github.com.keygripreloaded.common.scene.action.Action;

public class WindowTimeline extends Window
{
    public static final int ID_NEW_ACTION = 0;
    public static final int ID_EDIT_ACTION = 1;
    public static final int ID_DEL_ACTION = 2;
    public static final int ID_REC_ACTION = 3;
    public static final int ID_PLAY_SCENE = 4;
    public static final int ID_STOP_SCENE = 5;

    public GuiWorkspace parent;

    public ElementTimeline timeline;

    public WindowTimeline(GuiWorkspace parent, int x, int y, int w, int h, int minW, int minH)
    {
        super(parent, x, y, w, h, minW, minH, "window.timeline.title", true);
        this.parent = parent;
        timeline = new ElementTimeline(this, 1, 13, width - 2, height - 14, -2);
        elements.add(timeline);

        int button = 0;
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_NEW_ACTION, true, 0, 1, "window.timeline.newAction", new ResourceLocation("keygripreloaded", "textures/icon/new_action.png")));
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_EDIT_ACTION, true, 0, 1, "window.timeline.editAction", new ResourceLocation("keygripreloaded", "textures/icon/edit_action.png")));
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_DEL_ACTION, true, 0, 1, "window.timeline.delAction", new ResourceLocation("keygripreloaded", "textures/icon/del_action.png")));
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_REC_ACTION, true, 0, 1, "window.timeline.recAction", new ResourceLocation("keygripreloaded", "textures/icon/rec_action.png")));
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_PLAY_SCENE, true, 0, 1, "window.timeline.playScene", new ResourceLocation("keygripreloaded", "textures/icon/play_scene.png")));
        elements.add(new ElementButtonTextured(this, 20 * button++ + 1, 80, ID_STOP_SCENE, true, 0, 1, "window.timeline.stopScene", new ResourceLocation("keygripreloaded", "textures/icon/stop_scene.png")));
    }

    @Override
    public void elementTriggered(Element element)
    {
        if(!parent.hasOpenScene()) return;

        if(element.id == ID_NEW_ACTION)
        {
            workspace.addWindowOnTop(new WindowNewAction(workspace, workspace.width / 2 - 100, workspace.height / 2 - 80, 200, 220, 200, 220).putInMiddleOfScreen());
        } else if(element.id == ID_EDIT_ACTION && !parent.timeline.timeline.selectedIdentifier.isEmpty())
        {
            workspace.addWindowOnTop(new WindowEditAction(workspace, workspace.width / 2 - 100, workspace.height / 2 - 80, 200, 260, 200, 260).putInMiddleOfScreen());
        } else if(element.id == ID_DEL_ACTION && !parent.timeline.timeline.selectedIdentifier.isEmpty()) {
            Scene scene = parent.getOpenScene();
            for(int i = scene.actions.size() - 1; i >= 0; i--)
            {
                Action act = scene.actions.get(i);
                if(!act.identifier.equals(parent.timeline.timeline.selectedIdentifier)) continue;
                scene.actions.remove(i);
                parent.timeline.timeline.selectedIdentifier = "";
                Collections.sort(scene.actions);
                break;
            }
        } else if(element.id == ID_REC_ACTION) {
            if(parent.timeline.timeline.selectedIdentifier.isEmpty())
            {
                workspace.addWindowOnTop(new WindowPopup(workspace, 0, 0, 180, 80, 180, 80, "window.recAction.noAction").putInMiddleOfScreen());
            }
            parent.toggleRecording();
        } else if(element.id == ID_PLAY_SCENE) {
            if(parent.hasOpenScene() && parent.sceneSendingCooldown <= 0)
            {
                parent.timeline.timeline.setCurrentPos(0);
                if(GuiScreen.isCtrlKeyDown())
                {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                    Minecraft.getMinecraft().setIngameFocus();
                }
                Scene.sendSceneToServer(parent.getOpenScene());
            }
            parent.sceneSendingCooldown = 10;
        } else if(element.id == ID_STOP_SCENE) {
            if(KeygripReloaded.eventHandlerClient.actionToRecord != null)
            {
                parent.toggleRecording(); //Lets stop recording in case the end user doesn't know it's a record toggle button.
            } else if(parent.hasOpenScene()) {
                parent.getOpenScene().stop();
                KeygripReloaded.channel.sendToServer(new PacketStopScene(parent.getOpenScene().identifier));
            }
        }
    }

    @Override
    public boolean canBeDragged()
    {
        return false;
    }

    @Override
    public int clickedOnBorder(int mouseX, int mouseY, int id)//only left clicks
    {
        if(id == 0 && !minimized)
        {
            return ((mouseY <= BORDER_SIZE + 1) ? 1 : 0) + 1; //you can only drag the top
        }
        return 0;
    }

    @Override
    public void setScissor()
    {
        RendererHelper.startGlScissor(posX, posY + 1, getWidth(), getHeight());
    }

    @Override
    public boolean invertMinimizeSymbol()
    {
        return true;
    }

    @Override
    public boolean interactableWhileNoProjects()
    {
        return false;
    }
}
