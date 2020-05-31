package tgm.github.com.keygripreloaded.common.scene;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketEntityTeleport;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.ichun.mods.ichunutil.client.gui.window.WindowPopup;
import me.ichun.mods.ichunutil.common.core.util.IOUtil;
import tgm.github.com.keygripreloaded.client.core.ResourceHelper;
import tgm.github.com.keygripreloaded.client.gui.GuiWorkspace;
import tgm.github.com.keygripreloaded.common.KeygripReloaded;
import tgm.github.com.keygripreloaded.common.packet.PacketSceneFragment;
import tgm.github.com.keygripreloaded.common.scene.action.Action;
import tgm.github.com.keygripreloaded.common.scene.action.LimbComponent;

public class Scene
{
    public static final transient int VERSION = 1;
    public static final transient int PRECISION = 1000;
    //Used names = a, i, n, v

    @SerializedName("n")
    public String name;
    @SerializedName("i")
    public String identifier;
    @SerializedName("v")
    public int version = VERSION;

    @SerializedName("a")
    public ArrayList<Action> actions = new ArrayList<>();//sort the list after
    @SerializedName("s")
    public int[] startPos = new int[3];

    public transient File saveFile;
    public transient String saveFileMd5;

    public transient int playTime;
    public transient boolean playing;

    public transient WorldServer server;

    public Scene(String name)
    {
        this.name = name;
        this.identifier = RandomStringUtils.randomAscii(IOUtil.IDENTIFIER_LENGTH);
    }

    public void update()
    {
        if(!playing) return;
        if(server == null) {
            playTime++;
            return;
        }
        actions.stream()
                .filter(action -> action.getLength() > 0)
                .forEach(a -> {
                    if(playTime > a.startKey + a.getLength() || playTime < a.startKey || a.hidden == 1) {
                        if(playTime == 5 && a.precreateEntity == 1 && a.state != null && a.state.ent != null) {
                            FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayersInDimension(new SPacketEntityTeleport(a.state.ent), a.state.ent.world.provider.getDimension());
                        }
                        return;
                    }
                    if(playTime == a.startKey && a.precreateEntity != 1 && a.createState(server, (startPos[0] + a.offsetPos[0]) / (double)PRECISION, (startPos[1] + a.offsetPos[1]) / (double)PRECISION, (startPos[2] + a.offsetPos[2]) / (double)PRECISION)) {
                        if(a.state.ent == null) {
                            KeygripReloaded.LOGGER.warn("Error initializing action: " + a.name);
                        } else {
                            a.state.ent.setPositionAndRotation((startPos[0] + a.offsetPos[0]) / (double)PRECISION, (startPos[1] + a.offsetPos[1]) / (double)PRECISION, (startPos[2] + a.offsetPos[2]) / (double)PRECISION, a.rotation[0] / (float)PRECISION, a.rotation[1] / (float)PRECISION);
                            a.state.ent.setUniqueId(UUID.randomUUID());
                            server.spawnEntity(a.state.ent);
                            if(a.state.ent instanceof EntityPlayer) {
                                server.playerEntities.remove(a.state.ent);
                                server.updateAllPlayersSleepingFlag();
                            }
                        }
                    }
                    else if(playTime == a.startKey + a.getLength() && a.persistEntity != 1 && a.state != null && a.state.ent != null) {
                        a.state.ent.setDead();
                        a.state.additionalEnts.forEach(Entity::setDead);
                        a.state = null;
                    }
                    a.doAction(this, playTime);
                    if(playTime - a.startKey == 5 && a.state != null && a.state.ent != null && a.precreateEntity != 1) {
                        FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendPacketToAllPlayersInDimension(new SPacketEntityTeleport(a.state.ent), a.state.ent.world.provider.getDimension());
                    }
                });
            playTime++;
    }

    public int getLength()
    {
        return actions.parallelStream().mapToInt(action-> action.startKey+action.getLength()).max().orElse(0);
    }

    public void create(WorldServer world)
    {
        server = world;
        actions.forEach(a -> {
            if(playTime > a.startKey + a.getLength() && a.persistEntity != 1 || a.hidden == 1) return;
            if(playTime >= a.startKey || a.precreateEntity != 1) return;
            //create the related entity
            if(!a.createState(world, (startPos[0] + a.offsetPos[0]) / (double)PRECISION, (startPos[1] + a.offsetPos[1]) / (double)PRECISION, (startPos[2] + a.offsetPos[2]) / (double)PRECISION)) {
                return;
            }

            if(a.state.ent == null) {
                KeygripReloaded.LOGGER.warn("Error initializing action: " + a.name);
                return;
            }

            a.state.ent.setUniqueId(UUID.randomUUID());

            a.state.ent.setPositionAndRotation((startPos[0] + a.offsetPos[0]) / (double)PRECISION, (startPos[1] + a.offsetPos[1]) / (double)PRECISION, (startPos[2] + a.offsetPos[2]) / (double)PRECISION, a.rotation[0] / (float)PRECISION, a.rotation[1] / (float)PRECISION);
            world.spawnEntity(a.state.ent);
            if(a.state.ent instanceof EntityPlayer) {
                world.playerEntities.remove(a.state.ent);
                world.updateAllPlayersSleepingFlag();
            }

            IntStream.range(0, a.state.inventory.length)
                    .filter(i -> a.state.inventory[i] != null)
                    .boxed()
                    .collect(Collectors.toMap(i -> i+1, i -> a.state.inventory[i]))
                    .forEach((i, itemStack) -> a.state.ent.setItemStackToSlot(Action.convertSlotNumToEnum(i), itemStack));

            LimbComponent lastLook = null;
            LimbComponent lastPos = null;
            int lastLookInt = -1;

            for(Map.Entry<Integer, LimbComponent> e : a.lookComponents.entrySet()) {
                if(e.getKey() <= lastLookInt || e.getKey() >= playTime) continue;

                lastLookInt = e.getKey();
                lastLook = e.getValue();
            }
            int lastPosInt = -1;
            for(Map.Entry<Integer, LimbComponent> e : a.posComponents.entrySet())
            {
                if(e.getKey() <= lastPosInt || e.getKey() >= playTime) continue;

                lastPosInt = e.getKey();
                lastPos = e.getValue();
            }
            if(lastLook != null)
            {
                a.state.ent.rotationYawHead = a.state.ent.rotationYaw = (float) lastLook.actionChange[0] / Scene.PRECISION;
                a.state.ent.rotationPitch = (float) lastLook.actionChange[1] / Scene.PRECISION;
            }
            if(lastPos != null)
            {
                a.state.ent.setLocationAndAngles((lastPos.actionChange[0] + (a.offsetPos[0] + startPos[0])) / (double)PRECISION, (lastPos.actionChange[1] + (a.offsetPos[1] + startPos[1])) / (double)PRECISION, (lastPos.actionChange[2] + (a.offsetPos[2] + startPos[2])) / (double)PRECISION, a.state.ent.rotationYaw, a.state.ent.rotationPitch);
            }
        });
    }

    public void play()
    {
        playing = true;
    }

    public void stop()
    {
        actions.stream()
                .filter(action -> action.state != null && action.state.ent != null)
                .forEach(action -> {
                    action.state.ent.setDead();
                    action.state.additionalEnts.forEach(Entity::setDead);
                });
        playing = false;
    }

    public void destroy()
    {
        actions.stream()
                .filter(action -> action.state != null && action.state.ent != null)
                .forEach(action -> {
                    action.state.ent.setDead();
                    action.state.additionalEnts.forEach(Entity::setDead);
                });
    }

    public void repair()
    {
        //not needed yet, save files are still first edition.
    }

    public static Scene openScene(File file)
    {
        try
        {
            //            InputStream con = new FileInputStream(file);
            //            String data = new String(ByteStreams.toByteArray(con));
            //            con.close();
            //
            //            Scene scene = (new Gson()).fromJson(data, Scene.class);
            //
            //            scene.saveFile = (file);
            //            scene.saveFileMd5 = IOUtil.getMD5Checksum(file);
            //            scene.repair();
            //
            //            return scene;

            byte[] data = new byte[(int)file.length()];
            FileInputStream stream = new FileInputStream(file);
            stream.read(data);
            stream.close();

            Scene scene = (new Gson()).fromJson(IOUtil.decompress(data), Scene.class);

            scene.saveFile = (file);
            scene.saveFileMd5 = IOUtil.getMD5Checksum(file);
            scene.repair();

            return scene;
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean saveScene(Scene scene, File file)
    {
        try
        {
            //            FileUtils.writeStringToFile(file, (new Gson()).toJson(scene));

            FileOutputStream stream = new FileOutputStream(file);
            stream.write(IOUtil.compress((new Gson()).toJson(scene)));
            stream.close();

            return true;
        }
        catch(IOException ignored) {}
        return false;
    }

    public static void saveSceneActions(Scene scene)
    {
        ArrayList<String> actNames = new ArrayList<>();
        for(Action action : scene.actions)
        {
            try
            {
                String name = action.name;
                int append = 0;
                while(actNames.contains(name))
                {
                    if(append != 0)
                    {
                        name = name.substring(0, name.length() - 2);
                    }
                    append++;
                    name = name + "_" + append;
                }
                FileOutputStream stream = new FileOutputStream(new File(ResourceHelper.getActionsDir(), scene.name + "-" + name + ".kga"));
                stream.write(IOUtil.compress((new Gson()).toJson(action)));
                stream.close();
                actNames.add(name);
            }
            catch(IOException ignored) {}
        }
    }

    @SideOnly(Side.CLIENT)
    public static void sendSceneToServer(Scene scene)
    {
        File temp = new File(ResourceHelper.getTempDir(), Math.abs(scene.hashCode()) + "-send.kgs");

        if(Scene.saveScene(scene, temp)) {
            try {
                byte[] data = IOUtils.toByteArray(new FileInputStream(temp));

                final int maxFile = 31000; //smaller packet cause I'm worried about too much info carried over from the bloat vs hat info.

                int fileSize = data.length;

                int packetsToSend = (int)Math.ceil((float)fileSize / (float)maxFile);

                int packetCount = 0;
                int offset = 0;
                while(fileSize > 0)
                {
                    byte[] fileBytes = new byte[Math.min(fileSize, maxFile)];
                    int index = 0;
                    while(index < fileBytes.length) //from index 0 to 31999
                    {
                        fileBytes[index] = data[index + offset];
                        index++;
                    }

                    int time = 0;

                    if(Minecraft.getMinecraft().currentScreen instanceof GuiWorkspace) {
                        //open popup
                        GuiWorkspace workspace = (GuiWorkspace)Minecraft.getMinecraft().currentScreen;
                        time = workspace.timeline.timeline.getCurrentPos();
                        if(time > scene.getLength()) {
                            time = 0;
                        }
                    }

                    KeygripReloaded.channel.sendToServer(new PacketSceneFragment(time, scene.identifier, packetsToSend, packetCount, Math.min(fileSize, maxFile), fileBytes));

                    packetCount++;
                    fileSize -= maxFile;
                    offset += index;
                }
            }
            catch(IOException ignored) {}
            temp.delete();
        }
        else if(Minecraft.getMinecraft().currentScreen instanceof GuiWorkspace) {
            //open popup
            GuiWorkspace workspace = (GuiWorkspace)Minecraft.getMinecraft().currentScreen;
            workspace.addWindowOnTop(new WindowPopup(workspace, 0, 0, 180, 80, 180, 80, "window.playScene.failed").putInMiddleOfScreen());
        }
    }
}
