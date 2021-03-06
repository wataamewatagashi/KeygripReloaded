package tgm.github.com.keygripreloaded.common.scene.action;

import com.google.gson.annotations.SerializedName;

public class ActionComponent
{
    public ActionComponent(int type, int itemAct, byte[] itemTag)
    {
        this.toggleType = type;
        this.itemAction = itemAct;
        this.itemNBT = itemTag;
    }

    @SerializedName("t")
    public int toggleType; //1 = itemuse, 2 = sprinting, 3 = sneaking, 4 = swinging, 5 = healthchange, 6 = fire, 7 = sleeping, 8 = elytraflying;
    @SerializedName("iA")
    public int itemAction; //0 for nothing, 1 = equipped, 2-5 armor, 6 = swaphand, 7 = dropitem;
    @SerializedName("iN")
    public byte[] itemNBT;
}
