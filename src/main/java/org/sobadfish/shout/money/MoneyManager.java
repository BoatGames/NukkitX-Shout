package org.sobadfish.shout.money;


import me.onebone.economyapi.EconomyAPI;

public class MoneyManager {

    private final EconomyAPI api;

    public static MoneyManager getManager(){
        try {

            Class.forName("me.onebone.economyapi.EconomyAPI");
            return new MoneyManager(EconomyAPI.getInstance());
        } catch (ClassNotFoundException e) {
            return new MoneyManager(null);
        }
    }

    private MoneyManager(EconomyAPI o){
        api = o;
    }

    public boolean addMoney(String player,double money){
        if(api != null){
            api.addMoney(player, money);
            return true;
        }
         return false;
    }

    public float myMoney(String player){
        if(api != null){
            return (float) api.myMoney(player);
        }
        return 0;
    }

    public boolean reduceMoney(String player,double money){
        if(api != null){
            api.reduceMoney(player, money);
            return true;
        }
        return false;
    }
}
