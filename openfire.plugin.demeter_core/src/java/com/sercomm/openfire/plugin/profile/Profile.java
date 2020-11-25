package com.sercomm.openfire.plugin.profile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Profile
{
    public static IProfile get(String modelName)
    {
        Model model = Model.fromString(modelName);
        
        IProfile profile;
        switch(model)
        {
            case VOX30:
                profile = new VOX30();
                break;
            case VOX30ONT:
                profile = new VOX30ONT();
                break;
            case HG4234B:
                profile = new HG4234B();
                break;
            case HG5244B:
                profile = new HG5244B();
                break;
            case FG4234B:
                profile = new FG4234B();
                break;
            case FG5244B:
                profile = new FG5244B();
                break;
            case RV6699AZ:
                profile = new RV6699AZ();
                break;
            case SC0001:
            case IPQ8074_AP_HK01:
                profile = new IPQ8074_AP_HK01();
                break;
            case S3:
                profile = new S3();
                break;
            case S3RT:
                profile = new S3RT();
                break;
            case S3N:
                profile = new S3N();
                break;
            default:
                profile = null;
                break;
        }
        
        return profile;
    }

    public enum Model
    {
        VOX30("VOX30"),
        VOX30ONT("VOX30ONT"),
        HG4234B("HG4234B"),
        HG5244B("HG5244B"),
        FG4234B("FG4234B"),
        FG5244B("FG5244B"),
        RV6699AZ("RV6699AZ"),
        SC0001("SC0001"),
        IPQ8074_AP_HK01("IPQ8074 AP.HK01"),
        S3("S3"),
        S3RT("S3RT"),
        S3N("S3N");
        
        private static Map<String, Model> map = 
                new ConcurrentHashMap<String, Model>();
        static
        {
            for(Model appAction : Model.values())
            {
                map.put(appAction.toString(), appAction);
            }
        }
        
        private String value;
        private Model(String value)
        {
            this.value = value;
        }
            
        @Override
        public String toString()
        {
            return this.value;
        }

        public static Model fromString(String value)
        {
            return map.get(value);
        }
    }    
}
