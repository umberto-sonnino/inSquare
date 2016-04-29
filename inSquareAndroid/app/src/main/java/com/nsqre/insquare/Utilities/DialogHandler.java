package com.nsqre.insquare.Utilities;/* Created by umbertosonnino on 8/4/16  */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.model.LatLng;
import com.nsqre.insquare.Activities.CreateSquareActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.Square.RecyclerProfileSquareAdapter;
import com.nsqre.insquare.Square.Square;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.io.ByteArrayOutputStream;
import java.util.Date;

/**
 * TODO Documentare
 */
public class DialogHandler {

    public static final String SQUARE_TYPE = "SQUARE_TYPE";
    public static final String MAP_SCREENSHOT = "MAP_SCREENSHOT";

    public static void handleCreateOptions(
            final Context where,
            final String TAG,
            LatLng latLng,
            Bitmap mapScreenshot)
    {
        final CharSequence options[] = new CharSequence[]{
                "Luogo", //0
                "Evento", //1
                "Attività Commerciale", //2
        };

        final String lat = Double.toString(latLng.latitude);
        final String lon = Double.toString(latLng.longitude);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        mapScreenshot.compress(Bitmap.CompressFormat.PNG, 100, stream);
        final byte[] screenshotData = stream.toByteArray();

        AlertDialog.Builder builder = new AlertDialog.Builder(where);
        builder.setTitle("Crea una Square");
        builder.setItems(options,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(where, CreateSquareActivity.class);
                        intent.putExtra(SQUARE_TYPE, which);
                        intent.putExtra(MAP_SCREENSHOT, screenshotData);
                        where.startActivity(intent);
                    }
        });

        builder.show();

    }

    public static void handleMuteRequest(
            final Context where,
            final View snackbarContainer,
            final String TAG,
            final String squareId
    )
    {
        final CharSequence options[] = new CharSequence[]{
                "Abilita", //0
                "Disabilita per 1h", //1
                "Disabilita per 8h", //2
                "Disabilita per 2 giorni",//3
                "Disabilita indefinitamente"//4
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(where);
        builder.setTitle("Gestisci notifiche");
        builder.setItems(options,
                new DialogInterface.OnClickListener() {

                    boolean valid = true;

                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        Snackbar showResult = Snackbar.make(snackbarContainer, "", Snackbar.LENGTH_LONG);
                        Log.d(TAG, "onCheckedChanged: " + which + " " + options[which]);
                        if (which == 0) {
                            showResult.setText("Notifiche Abilitate");
                        } else {
                            showResult.setText("Notifiche Disabilitate");
                        }

                        showResult.setAction("Annulla",
                                new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        valid = false;
                                    }
                                });
                        showResult.setCallback(
                                new Snackbar.Callback() {
                                    @Override
                                    public void onDismissed(Snackbar snackbar, int event) {
                                        super.onDismissed(snackbar, event);
                                        if(valid) {
                                            SharedPreferences sharedPreferences = where.getSharedPreferences("NOTIFICATION_MUTE_MAP", where.MODE_PRIVATE);

                                            if(sharedPreferences.contains(squareId)){
                                                sharedPreferences.edit().remove(squareId).apply();
                                            }

                                            long expireTime = 0;

                                            long minutes = 1000 * 60;
                                            long hours = minutes * 60;
                                            long days = hours * 24;

                                            long years = 365 * days;

                                            switch (which)
                                            {
                                                case 0:
                                                    expireTime = new Date().getTime() - hours;
                                                    break;
                                                case 1:
                                                    expireTime = new Date().getTime() + hours;
                                                    break;
                                                case 2:
                                                    expireTime = new Date().getTime() + 8 * hours;
                                                    break;
                                                case 3:
                                                    expireTime = new Date().getTime() + 2 * days;
                                                    break;
                                                case 4:
                                                    expireTime = new Date().getTime() + 10 * years;
                                                    break;
                                            }
                                            sharedPreferences.edit().putString(squareId, Long.toString(expireTime)).apply();
/*
                                            VolleyManager.getInstance().muteSquare(
                                                    InSquareProfile.getUserId(),
                                                    squareId,
                                                    which,
                                                    new VolleyManager.VolleyResponseListener() {
                                                        @Override
                                                        public void responseGET(Object object) {
                                                            // Rimane vuota
                                                        }

                                                        @Override
                                                        public void responsePOST(Object object) {
                                                            // Rimane vuota
                                                        }

                                                        @Override
                                                        public void responsePATCH(Object object) {
                                                            boolean muteResponse = (boolean) object;
                                                            if(muteResponse)
                                                            {

                                                            }else
                                                            {

                                                            }
                                                        }

                                                        @Override
                                                        public void responseDELETE(Object object) {
                                                            // Rimane vuota
                                                        }
                                                    }
                                            );*/
                                        }
                                        else {
                                            Log.d(TAG, "onDismissed: well, I've been undone!");
                                        }
                                    }
                                }
                        );

                        showResult.show();
                    }
                });

        builder.show();
    }

    public static void handleProfileEdit(
            final Square element,
            final RecyclerProfileSquareAdapter.SquareViewHolder squareViewHolder,
            final View parentLayout,
            final String TAG
    ) {
        String oldDescription = element.getDescription().trim();
        String oldName = element.getName().trim();

        final Dialog editDialog = new Dialog(parentLayout.getContext());
        editDialog.setContentView(R.layout.dialog_edit_square);
        editDialog.setCancelable(true);
        editDialog.show();

        final EditText nameEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_name_text);

        ((TextInputLayout)nameEditText.getParent()).setHint("Modifica il nome");
        nameEditText.setText(oldName);

        final EditText descriptionEditText = (EditText) editDialog.findViewById(R.id.dialog_edit_description_text);
        if(!oldDescription.isEmpty())
        {
            ((TextInputLayout)descriptionEditText.getParent()).setHint("Modifica la descrizione");
            descriptionEditText.setText(oldDescription);
        }

        final Button okButton = (Button) editDialog.findViewById(R.id.dialog_edit_ok_button);
        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final String newDescription = descriptionEditText.getText().toString().trim();
                        final String newName = nameEditText.getText().toString().trim();

                        if (newName.isEmpty()) {
//                            Toast.makeText(where, "Il nome non può essere vuoto!", Toast.LENGTH_SHORT).show();
//                            Snackbar.make(parentLayout, "Il nome non può essere vuoto", Snackbar.LENGTH_SHORT).show();
                            ((TextInputLayout)nameEditText.getParent()).setErrorEnabled(true);
                            ((TextInputLayout)nameEditText.getParent()).setError("Il nome non può essere vuoto");
                            return;
                        }

                        Log.d(TAG, "onClick: stai tentando di modificare la descrizione:\n" + newDescription);
                        Log.d(TAG, "onClick: stai tentando di modificare il nome:\n" + newName);
                        VolleyManager.getInstance().patchDescription(newName, newDescription, element.getId(), InSquareProfile.getUserId(),
                                new VolleyManager.VolleyResponseListener() {
                                    @Override
                                    public void responseGET(Object object) {
                                        // Lasciare vuoto
                                    }

                                    @Override
                                    public void responsePOST(Object object) {
                                        // Lasciare vuoto

                                    }

                                    @Override
                                    public void responsePATCH(Object object) {
                                        boolean response = (boolean) object;
                                        if (response) {
                                            // Tutto OK!
                                            Log.d(TAG, "responsePATCH: sono riuscito a patchare correttamente!");
                                            squareViewHolder.squareName.setText(newName);
                                            squareViewHolder.squareDescription.setText(newDescription);
                                            element.setName(newName);
                                            element.setDescription(newDescription);

                                            if (newDescription.isEmpty()) {
                                                squareViewHolder.squareDescription.setVisibility(View.GONE);
                                            } else {
                                                squareViewHolder.squareDescription.setVisibility(View.VISIBLE);
                                            }
//                                            Toast.makeText(where, "Modificata con successo!", Toast.LENGTH_SHORT).show();
                                            Snackbar.make(parentLayout, "Modificata con successo!", Snackbar.LENGTH_SHORT).show();
                                            editDialog.dismiss();
                                        } else {
                                            // Errore
//                                            Toast.makeText(where, "Ho avuto un problema con la connessione. Riprova..?", Toast.LENGTH_SHORT).show();
                                            Snackbar.make(parentLayout, "Ho avuto un problema con la connessione. Riprova..?", Snackbar.LENGTH_SHORT).show();
                                            editDialog.dismiss();
                                        }
                                    }

                                    @Override
                                    public void responseDELETE(Object object) {
                                        // Lasciare vuoto
                                    }
                                });
                    }
                }
        );

    }



}
