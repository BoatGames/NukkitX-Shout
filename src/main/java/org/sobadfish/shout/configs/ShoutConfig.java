package org.sobadfish.shout.configs;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;


public class ShoutConfig {

    public MsgDTO msg;

    public int money;

    public RateDTO rate;

    public static class MsgDTO {

        public String title;

        public String subTitle;

        public String input;

        public ChoseDTO chose;

        public Chose1DTO chose1;


        public static class ChoseDTO {

            public String title;

            public List<String> list;
        }


        public static class Chose1DTO {

            public String title;

            public List<String> list;
        }
    }

    public static class RateDTO {

        public Integer msg;

        public Integer custom;

        public Double random;
    }
}
