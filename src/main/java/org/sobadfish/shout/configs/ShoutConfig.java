package org.sobadfish.shout.configs;

import java.util.List;

public class ShoutConfig {

    public Cfg msg;

    public double money;

    public Rate rate;

    public static class Rate{
        public float msg;

        public float custom;

        public float random;


    }

    public static class Cfg{
        public String title;

        public String subTitle;

        public String input;

        public FromChose chose;


        public FromChose chose1;


        public static class FromChose{
            public String title;

            public List<String> list;
        }



    }

}
