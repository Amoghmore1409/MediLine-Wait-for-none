package com.example.mediline;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.signed.SignatureProvider;
import com.cloudinary.android.signed.Signature;
import com.cloudinary.Cloudinary;

import java.util.HashMap;
import java.util.Map;

public class MediLineApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "db2eubwvd");
            config.put("api_key", "684913487915397");
            config.put("api_secret", "wreN1NzspJayiM-uk62Jvd86XIQ");

            MediaManager.init(this, new SignatureProvider() {
                @Override
                public Signature provideSignature(Map options) {
                    try {
                        Cloudinary cloudinary = new Cloudinary(config);
                        String apiSecret = "wreN1NzspJayiM-uk62Jvd86XIQ";
                        // Filter out non-string keys if necessary, or just pass options
                        Map<String, Object> castOptions = (Map<String, Object>) options;
                        String signature = cloudinary.apiSignRequest(castOptions, apiSecret);
                        
                        Object timestampObj = options.get("timestamp");
                        long timestamp = 0;
                        if (timestampObj instanceof Long) {
                            timestamp = (Long) timestampObj;
                        } else if (timestampObj instanceof String) {
                            timestamp = Long.parseLong((String) timestampObj);
                        } else if (timestampObj != null) {
                            timestamp = Long.parseLong(timestampObj.toString());
                        }

                        return new Signature(signature, "684913487915397", timestamp);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public String getName() {
                    return "MySignatureProvider";
                }
            }, config);
        } catch (Exception e) {
            // MediaManager is already initialized
        }
    }
}
