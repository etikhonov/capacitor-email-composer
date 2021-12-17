package de.einfachhans.emailcomposer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONException;

@CapacitorPlugin(name = "EmailComposer")
public class EmailComposerPlugin extends Plugin {

    private EmailComposer implementation = new EmailComposer();

    @PluginMethod
    public void hasAccount(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("hasAccount", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void open(PluginCall call) throws JSONException {
        try {
            Intent draft = implementation.getIntent(call, this.getContext());
            startActivityForResult(call, draft, "openCallback");
        } catch (ActivityNotFoundException anf) {
            call.reject("No Activity found to send E-Mail");
        } catch (IllegalArgumentException illegalArgument) {
            call.reject(illegalArgument.getMessage());
        }
    }

    @ActivityCallback
    private void openCallback(PluginCall call, ActivityResult result) {
        call.resolve();
    }
}
