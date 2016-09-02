package me.ccrama.redditslide.Views;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.html.HtmlRenderer;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import me.ccrama.redditslide.Activities.Draw;
import me.ccrama.redditslide.Activities.MainActivity;
import me.ccrama.redditslide.ColorPreferences;
import me.ccrama.redditslide.Constants;
import me.ccrama.redditslide.Drafts;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SecretConstants;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.SpoilerRobotoTextView;
import me.ccrama.redditslide.util.LogUtil;
import me.ccrama.redditslide.util.RealPathUtil;
import me.ccrama.redditslide.util.SubmissionParser;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by carlo_000 on 10/18/2015.
 */
public class DoEditorActions {

    public static void doActions(final EditText editText, final View baseView,
            final FragmentManager fm, final Activity a, final String oldComment) {
        baseView.findViewById(R.id.bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.hasSelection()) {
                    wrapString("**",
                            editText); //If the user has text selected, wrap that text in the symbols
                } else {
                    //If the user doesn't have text selected, put the symbols around the cursor's position
                    int pos = editText.getSelectionStart();
                    editText.getText().insert(pos, "**");
                    editText.getText().insert(pos + 1, "**");
                    editText.setSelection(pos + 2); //put the cursor between the symbols
                }
            }
        });

        baseView.findViewById(R.id.italics).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.hasSelection()) {
                    wrapString("*",
                            editText); //If the user has text selected, wrap that text in the symbols
                } else {
                    //If the user doesn't have text selected, put the symbols around the cursor's position
                    int pos = editText.getSelectionStart();
                    editText.getText().insert(pos, "*");
                    editText.getText().insert(pos + 1, "*");
                    editText.setSelection(pos + 1); //put the cursor between the symbols
                }
            }
        });

        baseView.findViewById(R.id.strike).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText.hasSelection()) {
                    wrapString("~~",
                            editText); //If the user has text selected, wrap that text in the symbols
                } else {
                    //If the user doesn't have text selected, put the symbols around the cursor's position
                    int pos = editText.getSelectionStart();
                    editText.getText().insert(pos, "~~");
                    editText.getText().insert(pos + 1, "~~");
                    editText.setSelection(pos + 2); //put the cursor between the symbols
                }
            }
        });

        baseView.findViewById(R.id.savedraft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.v("Saving draft");
                Drafts.addDraft(editText.getText().toString());
                Snackbar s = Snackbar.make(baseView.findViewById(R.id.savedraft), "Draft saved",
                        Snackbar.LENGTH_SHORT);
                View view = s.getView();
                TextView tv =
                        (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                tv.setTextColor(Color.WHITE);
                s.show();
            }
        });
        baseView.findViewById(R.id.draft).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ArrayList<String> drafts = Drafts.getDrafts();
                Collections.reverse(drafts);
                final String[] draftText = new String[drafts.size()];
                for (int i = 0; i < drafts.size(); i++) {
                    draftText[i] = drafts.get(i);
                }
                if (drafts.isEmpty()) {
                    new AlertDialogWrapper.Builder(a).setTitle(R.string.dialog_no_drafts)
                            .setMessage(R.string.dialog_no_drafts_msg)
                            .setPositiveButton(R.string.btn_ok, null)
                            .show();
                } else {
                    new AlertDialogWrapper.Builder(a).setTitle(R.string.choose_draft)
                            .setItems(draftText, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    editText.setText(
                                            editText.getText().toString() + draftText[which]);
                                }
                            })
                            .setNeutralButton(R.string.btn_cancel, null)
                            .setPositiveButton(R.string.manage_drafts,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            final boolean[] selected = new boolean[drafts.size()];
                                            new AlertDialogWrapper.Builder(a).setTitle(
                                                    R.string.choose_draft)
                                                    .setNeutralButton(R.string.btn_cancel, null)
                                                    .alwaysCallMultiChoiceCallback()
                                                    .setNegativeButton(R.string.btn_delete,
                                                            new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface dialog,
                                                                        int which) {
                                                                    new AlertDialogWrapper.Builder(
                                                                            a).setTitle(
                                                                            R.string.really_delete_drafts)
                                                                            .setCancelable(false)
                                                                            .setPositiveButton(
                                                                                    R.string.btn_yes,
                                                                                    new DialogInterface.OnClickListener() {
                                                                                        @Override
                                                                                        public void onClick(
                                                                                                DialogInterface dialog,
                                                                                                int which) {
                                                                                            ArrayList<String>
                                                                                                    draf =
                                                                                                    new ArrayList<>();
                                                                                            for (int
                                                                                                    i =
                                                                                                    0;
                                                                                                    i
                                                                                                            < draftText.length;
                                                                                                    i++) {
                                                                                                if (!selected[i]) {
                                                                                                    draf.add(
                                                                                                            draftText[i]);
                                                                                                }
                                                                                            }
                                                                                            Drafts.save(
                                                                                                    draf);
                                                                                        }
                                                                                    })
                                                                            .setNegativeButton(
                                                                                    R.string.btn_no,
                                                                                    null)
                                                                            .show();
                                                                }
                                                            })
                                                    .setMultiChoiceItems(draftText, selected,
                                                            new DialogInterface.OnMultiChoiceClickListener() {
                                                                @Override
                                                                public void onClick(
                                                                        DialogInterface dialog,
                                                                        int which,
                                                                        boolean isChecked) {
                                                                    selected[which] = isChecked;
                                                                }
                                                            })
                                                    .show();
                                        }
                                    })
                            .show();
                }
            }
        });
       /*todo baseView.findViewById(R.id.strikethrough).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wrapString("~~", editText);
            }
        });*/
        baseView.findViewById(R.id.imagerep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                if (a instanceof MainActivity) {
                    LogUtil.v("Running on main");
                    ((MainActivity) a).doImage = new Runnable() {
                        @Override
                        public void run() {
                            LogUtil.v("Running");
                            if (((MainActivity) a).data != null) {
                                Uri selectedImageUri = ((MainActivity) a).data.getData();
                                Log.v(LogUtil.getTag(), "WORKED! " + selectedImageUri.toString());
                                try {
                                    new UploadImgur(editText).execute(selectedImageUri);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    };
                    a.startActivityForResult(Intent.createChooser(intent,
                            Integer.toString(R.string.editor_select_img)), 3333);
                } else {
                    Fragment auxiliary = new AuxiliaryFragment();
                    Bundle data = new Bundle();
                    data.putInt("textId", editText.getId());
                    auxiliary.setArguments(data);
                    fm.beginTransaction().add(auxiliary, "IMAGE_CHOOSER").commit();
                    fm.executePendingTransactions();

                    auxiliary.startActivityForResult(Intent.createChooser(intent,
                            Integer.toString(R.string.editor_select_img)), 3333);
                }
            }
        });

        baseView.findViewById(R.id.draw).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SettingValues.tabletUI) {
                    doDraw(a, editText, fm);
                } else {
                    AlertDialogWrapper.Builder b = new AlertDialogWrapper.Builder(a).setTitle(
                            R.string.general_cropdraw_ispro)
                            .setMessage(R.string.pro_upgrade_msg)
                            .setPositiveButton(R.string.btn_yes_exclaim,

                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                            try {
                                                a.startActivity(new Intent(Intent.ACTION_VIEW,
                                                        Uri.parse(
                                                                "market://details?id=me.ccrama.slideforreddittabletuiunlock")));
                                            } catch (ActivityNotFoundException e) {
                                                a.startActivity(new Intent(Intent.ACTION_VIEW,
                                                        Uri.parse(
                                                                "http://play.google.com/store/apps/details?id=me.ccrama.slideforreddittabletuiunlock")));
                                            }
                                        }
                                    })
                            .setNegativeButton(R.string.btn_no_danks,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                            dialog.dismiss();
                                        }
                                    });
                    if (SettingValues.previews > 0) {
                        b.setNeutralButton(
                                a.getString(R.string.pro_previews, SettingValues.previews),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        SettingValues.prefs.edit()
                                                .putInt(SettingValues.PREVIEWS_LEFT,
                                                        SettingValues.previews - 1)
                                                .apply();
                                        SettingValues.previews = SettingValues.prefs.getInt(
                                                SettingValues.PREVIEWS_LEFT, 10);
                                        doDraw(a, editText, fm);
                                    }
                                });
                    }
                    b.show();
                }
            }
        });
       /*todo baseView.findViewById(R.id.superscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertBefore("^", editText);
            }
        });*/
        baseView.findViewById(R.id.size).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertBefore("#", editText);
            }
        });

        baseView.findViewById(R.id.quote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (oldComment != null) {
                    final TextView showText = new TextView(a);
                    showText.setText(oldComment);
                    showText.setTextIsSelectable(true);
                    int sixteen = Reddit.dpToPxVertical(24);
                    showText.setPadding(sixteen, 0, sixteen, 0);
                    AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(a);
                    builder.setView(showText)
                            .setTitle(R.string.editor_actions_quote_comment)
                            .setCancelable(true)
                            .setPositiveButton(a.getString(R.string.btn_select),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String selected = showText.getText()
                                                    .toString()
                                                    .substring(showText.getSelectionStart(),
                                                            showText.getSelectionEnd());
                                            if (selected.equals("")) {
                                                insertBefore("> " + oldComment, editText);
                                            } else {
                                                insertBefore("> " + selected + "\n\n", editText);
                                            }
                                        }
                                    })
                            .setNegativeButton(a.getString(R.string.btn_cancel), null)
                            .show();
                } else {
                    insertBefore("> ", editText);
                }
            }
        });

        baseView.findViewById(R.id.bulletlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertBefore("* ", editText);
            }
        });

        baseView.findViewById(R.id.numlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertBefore("1. ", editText);
            }
        });

        baseView.findViewById(R.id.preview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Extension> extensions =
                        Arrays.asList(TablesExtension.create(), StrikethroughExtension.create());
                Parser parser = Parser.builder().extensions(extensions).build();
                HtmlRenderer renderer = HtmlRenderer.builder().extensions(extensions).build();
                Node document = parser.parse(editText.getText().toString());
                String html = renderer.render(document);
                LayoutInflater inflater = a.getLayoutInflater();
                final View dialoglayout = inflater.inflate(R.layout.parent_comment_dialog, null);
                final AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(a);
                setViews(html, "NO sub",
                        (SpoilerRobotoTextView) dialoglayout.findViewById(R.id.firstTextView),
                        (CommentOverflow) dialoglayout.findViewById(R.id.commentOverflow));
                builder.setView(dialoglayout);
                builder.show();
            }
        });

        baseView.findViewById(R.id.link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final LayoutInflater inflater = LayoutInflater.from(a);
                final LinearLayout layout =
                        (LinearLayout) inflater.inflate(R.layout.insert_link, null);

                int[] attrs = {R.attr.font};

                TypedArray ta = baseView.getContext()
                        .obtainStyledAttributes(
                                new ColorPreferences(baseView.getContext()).getFontStyle()
                                        .getBaseId(), attrs);
                ta.recycle();

                String selectedText = "";
                //if the user highlighted text before inputting a URL, use that text for the descriptionBox
                if (editText.hasSelection()) {
                    final int startSelection = editText.getSelectionStart();
                    final int endSelection = editText.getSelectionEnd();

                    selectedText =
                            editText.getText().toString().substring(startSelection, endSelection);
                }

                final boolean selectedTextNotEmpty = !selectedText.isEmpty();

                final MaterialDialog dialog =
                        new MaterialDialog.Builder(editText.getContext()).title(
                                R.string.editor_title_link)
                                .customView(layout, false)
                                .positiveColorAttr(R.attr.tint)
                                .positiveText(R.string.editor_action_link)
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog,
                                            @NonNull DialogAction which) {
                                        final EditText urlBox =
                                                (EditText) dialog.findViewById(R.id.url_box);
                                        final EditText textBox =
                                                (EditText) dialog.findViewById(R.id.text_box);
                                        dialog.dismiss();

                                        final String s = "[".concat(textBox.getText().toString())
                                                .concat("](")
                                                .concat(urlBox.getText().toString())
                                                .concat(")");

                                        int start = Math.max(editText.getSelectionStart(), 0);
                                        int end = Math.max(editText.getSelectionEnd(), 0);

                                        editText.getText().insert(Math.max(start, end), s);

                                        //delete the selected text to avoid duplication
                                        if (selectedTextNotEmpty) {
                                            editText.getText().delete(start, end);
                                        }
                                    }
                                })
                                .build();

                //Tint the hint text if the base theme is Sepia
                if (SettingValues.currentTheme == 5) {
                    ((EditText) dialog.findViewById(R.id.url_box)).setHintTextColor(
                            ContextCompat.getColor(dialog.getContext(), R.color.md_grey_600));
                    ((EditText) dialog.findViewById(R.id.text_box)).setHintTextColor(
                            ContextCompat.getColor(dialog.getContext(), R.color.md_grey_600));
                }

                //use the selected text as the text for the link
                if (!selectedText.isEmpty()) {
                    ((EditText) dialog.findViewById(R.id.text_box)).setText(selectedText);
                }

                dialog.show();
            }
        });
    }

    public static void doDraw(final Activity a, final EditText editText, final FragmentManager fm) {
        Intent intent = new Intent(a, Draw.class);
        if (a instanceof MainActivity) {
            LogUtil.v("Running on main");
            ((MainActivity) a).doImage = new Runnable() {
                @Override
                public void run() {
                    LogUtil.v("Running");
                    if (((MainActivity) a).data != null) {
                        Uri selectedImageUri = ((MainActivity) a).data.getData();
                        Log.v(LogUtil.getTag(), "WORKED! " + selectedImageUri.toString());
                        try {
                            new UploadImgur(editText).execute(selectedImageUri);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            a.startActivityForResult(intent, 3333);
        } else {
            Fragment auxiliary = new AuxiliaryFragment();
            Bundle data = new Bundle();
            data.putInt("textId", editText.getId());
            auxiliary.setArguments(data);
            fm.beginTransaction().add(auxiliary, "IMAGE_CHOOSER").commit();
            fm.executePendingTransactions();

            auxiliary.startActivityForResult(intent, 3333);
        }
    }

    public static String getImageLink(Bitmap b) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, 100,
                baos); // Not sure whether this should be jpeg or png, try both and see which works best
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    public static void insertBefore(String wrapText, EditText editText) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().insert(Math.min(start, end), wrapText);
    }

    /* not using this method anywhere ¯\_(ツ)_/¯ */
//    public static void wrapNewline(String wrapText, EditText editText) {
//        int start = Math.max(editText.getSelectionStart(), 0);
//        int end = Math.max(editText.getSelectionEnd(), 0);
//        String s = editText.getText().toString().substring(Math.min(start, end), Math.max(start, end));
//        s = s.replace("\n", "\n" + wrapText);
//        editText.getText().replace(Math.min(start, end), Math.max(start, end), s);
//    }

    public static void wrapString(String wrapText, EditText editText) {
        int start = Math.max(editText.getSelectionStart(), 0);
        int end = Math.max(editText.getSelectionEnd(), 0);
        editText.getText().insert(Math.min(start, end), wrapText);
        editText.getText().insert(Math.max(start, end) + wrapText.length(), wrapText);
    }

    private static void setViews(String rawHTML, String subredditName,
            SpoilerRobotoTextView firstTextView, CommentOverflow commentOverflow) {
        if (rawHTML.isEmpty()) {
            return;
        }

        List<String> blocks = SubmissionParser.getBlocks(rawHTML);

        int startIndex = 0;
        // the <div class="md"> case is when the body contains a table or code block first
        if (!blocks.get(0).equals("<div class=\"md\">")) {
            firstTextView.setVisibility(View.VISIBLE);
            firstTextView.setTextHtml(blocks.get(0), subredditName);
            firstTextView.setLinkTextColor(
                    new ColorPreferences(firstTextView.getContext()).getColor(subredditName));
            startIndex = 1;
        } else {
            firstTextView.setText("");
            firstTextView.setVisibility(View.GONE);
        }

        if (blocks.size() > 1) {
            if (startIndex == 0) {
                commentOverflow.setViews(blocks, subredditName);
            } else {
                commentOverflow.setViews(blocks.subList(startIndex, blocks.size()), subredditName);
            }
        } else {
            commentOverflow.removeAllViews();
        }
    }

    private static class UploadImgur extends AsyncTask<Uri, Integer, JSONObject> {

        final         Context        c;
        final         EditText       editText;
        private final MaterialDialog dialog;
        public        Bitmap         b;

        public UploadImgur(EditText editText) {
            this.c = editText.getContext();
            this.editText = editText;
            dialog = new MaterialDialog.Builder(c).title(
                    c.getString(R.string.editor_uploading_image))
                    .progress(false, 100)
                    .cancelable(false)
                    .show();
        }

        public String getRealPathFromURI(Uri uri) {
            String[] projection = {MediaStore.Images.ImageColumns.DATA};
            Cursor metaCursor = c.getContentResolver().query(uri, projection, null, null, null);
            if (metaCursor != null) {
                try {
                    if (metaCursor.moveToFirst()) {
                        return metaCursor.getString(0);
                    }
                } finally {
                    metaCursor.close();
                }
            }
            return uri.getPath();
        }

        @Override
        protected JSONObject doInBackground(Uri... sub) {
            String path = getRealPathFromURI(sub[0]);
            LogUtil.v(path);
            File bitmap = new File(getRealPathFromURI(sub[0]));

            final OkHttpClient client = new OkHttpClient();

            try {
                RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("image", bitmap.getName(),
                                RequestBody.create(MediaType.parse("image/*"), bitmap))
                        .build();

                ProgressRequestBody body =
                        new ProgressRequestBody(formBody, new ProgressRequestBody.Listener() {
                            @Override
                            public void onProgress(int progress) {
                                publishProgress(progress);
                            }
                        });


                Request request = new Request.Builder().header("Authorization",
                        "Client-ID " + Constants.IMGUR_MASHAPE_CLIENT_ID)
                        .header("X-Mashape-Key", SecretConstants.getImgurApiKey(c))
                        .url("https://imgur-apiv3.p.mashape.com/3/image")
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return new JSONObject(response.body().string());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(final JSONObject result) {
            dialog.dismiss();
            try {
                int[] attrs = {R.attr.font};
                TypedArray ta = editText.getContext()
                        .obtainStyledAttributes(
                                new ColorPreferences(editText.getContext()).getFontStyle()
                                        .getBaseId(), attrs);
                final String url = result.getJSONObject("data").getString("link");
                LinearLayout layout = new LinearLayout(editText.getContext());
                layout.setOrientation(LinearLayout.VERTICAL);

                final TextView titleBox = new TextView(editText.getContext());
                titleBox.setText(url);
                layout.addView(titleBox);
                titleBox.setEnabled(false);
                titleBox.setTextColor(ta.getColor(0, Color.WHITE));

                final EditText descriptionBox = new EditText(editText.getContext());
                descriptionBox.setHint(R.string.editor_title);
                descriptionBox.setEnabled(true);
                descriptionBox.setTextColor(ta.getColor(0, Color.WHITE));


                ta.recycle();
                int sixteen = Reddit.dpToPxVertical(16);
                layout.setPadding(sixteen, sixteen, sixteen, sixteen);
                layout.addView(descriptionBox);
                new AlertDialogWrapper.Builder(editText.getContext()).setTitle(
                        R.string.editor_title_link)
                        .setView(layout)
                        .setPositiveButton(R.string.editor_action_link,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        String s = "["
                                                + descriptionBox.getText().toString()
                                                + "]("
                                                + url
                                                + ")";
                                        int start = Math.max(editText.getSelectionStart(), 0);
                                        int end = Math.max(editText.getSelectionEnd(), 0);
                                        editText.getText().insert(Math.max(start, end), s);
                                    }
                                })
                        .show();

            } catch (Exception e) {
                new AlertDialogWrapper.Builder(c).setTitle(R.string.err_title)
                        .setMessage(R.string.editor_err_msg)
                        .setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                e.printStackTrace();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            dialog.setProgress(values[0]);
            LogUtil.v("Progress:" + values[0]);
        }
    }

    public static class AuxiliaryFragment extends Fragment {
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (data != null) {
                Uri selectedImageUri = data.getData();
                Log.v(LogUtil.getTag(), "WORKED! " + selectedImageUri.toString());
                try {
                    new UploadImgur(((EditText) getActivity().findViewById(
                            getArguments().getInt("textId", 0)))).execute(selectedImageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
            }
        }
    }

    public static class ProgressRequestBody extends RequestBody {

        protected RequestBody  mDelegate;
        protected Listener     mListener;
        protected CountingSink mCountingSink;

        public ProgressRequestBody(RequestBody delegate, Listener listener) {
            mDelegate = delegate;
            mListener = listener;
        }

        @Override
        public MediaType contentType() {
            return mDelegate.contentType();
        }

        @Override
        public long contentLength() {
            try {
                return mDelegate.contentLength();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            mCountingSink = new CountingSink(sink);
            BufferedSink bufferedSink = Okio.buffer(mCountingSink);
            mDelegate.writeTo(bufferedSink);
            bufferedSink.flush();
        }

        protected final class CountingSink extends ForwardingSink {
            private long bytesWritten = 0;

            public CountingSink(Sink delegate) {
                super(delegate);
            }

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                bytesWritten += byteCount;
                mListener.onProgress((int) (100F * bytesWritten / contentLength()));
            }
        }

        public interface Listener {
            void onProgress(int progress);
        }
    }

}
