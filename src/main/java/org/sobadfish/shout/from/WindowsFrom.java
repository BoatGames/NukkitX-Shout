package org.sobadfish.shout.from;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.utils.TextFormat;
import org.sobadfish.shout.configs.ShoutConfig;

public class WindowsFrom implements Listener {

    public FromDataListener listener;

    public static final int ID = (int)(Math.random()*25565);

    public void setFromDataListener(FromDataListener listener){
        this.listener = listener;
    }

    public void displayWindow(Player player, ShoutConfig config){
        FormWindowCustom custom = new FormWindowCustom(TextFormat.colorize('&',config.msg.title));
        custom.addElement(new ElementLabel(TextFormat.colorize('&',config.msg.subTitle)));
        custom.addElement(new ElementInput(TextFormat.colorize('&',config.msg.input)));
        custom.addElement(new ElementDropdown(TextFormat.colorize('&',config.msg.chose.title),config.msg.chose.list));
        custom.addElement(new ElementDropdown(TextFormat.colorize('&',config.msg.chose1.title),config.msg.chose1.list));
        player.showFormWindow(custom,ID);
    }

    // 提交表单
    @EventHandler
    public void onFromListener(PlayerFormRespondedEvent event){
        if(event.wasClosed() || event.getResponse() == null){
            return;
        }
        if(event.getResponse().getClass() == FormResponseCustom.class && event.getFormID() == ID){
            FormResponseCustom responseCustom = (FormResponseCustom) event.getResponse();
            Player player = event.getPlayer();
            ShoutData data = new ShoutData();
            data.msg = responseCustom.getInputResponse(1);
            data.color = responseCustom.getDropdownResponse(2).getElementContent();
            data.type = responseCustom.getDropdownResponse(3).getElementContent();
            if(listener != null){
                listener.onData(player,data);
            }
        }
    }

    public static class ShoutData{
        public String msg;

        public String color;

        public String type;
    }

    public interface FromDataListener{
        void onData(Player player, ShoutData data);
    }
}
