package de.einfachhans.emailcomposer;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.Html;

import androidx.core.content.FileProvider;

import com.getcapacitor.PluginCall;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmailComposer {

    private static final String MAILTO_SCHEME = "mailto:";

    public Intent getIntent(PluginCall call, Context context) throws JSONException, ActivityNotFoundException, IllegalArgumentException {
        // Subject
        String subject = call.getString("subject", "");

        // Body
        String body = call.getString("body", "");
        CharSequence bodyText = call.getBoolean("isHtml", false) ? Html.fromHtml(body) : body;

        // To
        List<String> toList = call.getArray("to").toList();
        String[] to = new String[toList.size()];
        to = toList.toArray(to);

        // CC
        List<String> ccList = call.getArray("cc").toList();
        String[] cc = new String[ccList.size()];
        cc = ccList.toArray(cc);

        // BCC
        List<String> bccList = call.getArray("bcc").toList();
        String[] bcc = new String[bccList.size()];
        bcc = bccList.toArray(bcc);

        // Attachments
        if (call.hasOption("attachments")) {
            // Convert input data to Array<Uri>
            ArrayList<Uri> attachmentUris = new ArrayList<>();
            Object attachmentsValue = call.getData().opt("attachments");
            if (attachmentsValue instanceof JSONArray) {
                JSONArray valueArray = (JSONArray) attachmentsValue;
                for (int i = 0; i < valueArray.length(); i++) {
                    Object item = valueArray.get(i);
                    if (item instanceof String) {
                        Uri uri = getContentUriFromAbsolutePath(context,
                                (String) item);
                        attachmentUris.add(uri);
                    }
                }
            } else if (attachmentsValue instanceof String) {
                Uri uri = getContentUriFromAbsolutePath(context,
                        (String) attachmentsValue);
                attachmentUris.add(uri);
            } else {
                throw new IllegalArgumentException("Invalid data type for the" +
                        " \"attachment\" field");
            }

            // Create Intent
            if (attachmentUris.size() > 0) {
                return getIntentWithAttachments(context, attachmentUris, to,
                        cc, bcc, subject, bodyText);
            }
        }

        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(MAILTO_SCHEME));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, bodyText);
        intent.putExtra(Intent.EXTRA_EMAIL, to);
        intent.putExtra(Intent.EXTRA_CC, cc);
        intent.putExtra(Intent.EXTRA_BCC, bcc);
        return intent;
    }

    /**
     * Return an Intent for E-Mail with attachments
     *
     * @param context        - A Context for the current component.
     * @param attachmentUris - An Uri[] of attachments
     * @param to             - A String[] holding e-mail addresses that
     *                       should be delivered to.
     * @param cc             - A String[] holding e-mail addresses that
     *                       should be carbon
     *                       copied.
     * @param bcc            - A String[] holding e-mail addresses that
     *                       should be blind
     *                       carbon copied.
     * @param subject        - A constant string holding the desired subject
     *                       line of a
     *                       message.
     * @param body           - A CharSequence used to supply the literal data
     *                       to be sent.
     * @return An Intent for send E-Mail with attachments
     * @throws ActivityNotFoundException when no attachment supported email
     *                                   clients are installed
     * @throws IllegalArgumentException  when attachmentUris has no items
     */
    private Intent getIntentWithAttachments(
            Context context,
            ArrayList<Uri> attachmentUris,
            String[] to,
            String[] cc,
            String[] bcc,
            String subject,
            CharSequence body
    ) throws ActivityNotFoundException {
        if (attachmentUris.size() == 0) {
            throw new IllegalArgumentException("Empty attachmentUris " +
                    "Provided!");
        }

        List<Intent> intents = new ArrayList<>();
        String action = attachmentUris.size() > 1 ?
                Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                Uri.parse(MAILTO_SCHEME));
        List<ResolveInfo> resolvedEmailActivitiesInfo =
                context.getPackageManager().queryIntentActivities(emailIntent
                        , 0);
        for (ResolveInfo info : resolvedEmailActivitiesInfo) {
            Intent intent = new Intent(action);
            intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
            intent.putExtra(Intent.EXTRA_EMAIL, to);
            intent.putExtra(Intent.EXTRA_CC, cc);
            intent.putExtra(Intent.EXTRA_BCC, bcc);
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.putExtra(Intent.EXTRA_TEXT, body);
            if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
                        attachmentUris);
            } else {
                intent.putExtra(Intent.EXTRA_STREAM, attachmentUris.get(0));
            }
            intent.setType("*/*");
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intents.add(intent);
        }
        if (intents.size() == 1) {
            return intents.get(0);
        } else if (intents.size() > 1) {
            Intent chooser = Intent.createChooser(intents.remove(0), "");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    intents.toArray(new Intent[intents.size()]));
            return chooser;
        } else {
            throw new ActivityNotFoundException();
        }
    }

    /**
     * Return a content URI
     * for a given <b>String</b> representation of the absolute file path.
     *
     * @param context - A Context for the current component.
     * @param path    - An absolute path string.
     * @return A content URI for the file
     * @throws IllegalArgumentException When provided path isn't an absolute
     *                                  or one is outside the paths supported
     *                                  by the provider.
     */
    private Uri getContentUriFromAbsolutePath(Context context, String path) throws IllegalArgumentException {
        if (!path.startsWith("file:///")) {
            throw new IllegalArgumentException("Path param should be an " +
                    "absolute path");
        }
        File file = new File(path.replaceFirst("file://", ""));
        if (!file.exists()) {
            return Uri.EMPTY;
        }

        String authority = context.getPackageName() + ".fileprovider";
        return FileProvider.getUriForFile(context, authority, file);
    }
}
