package com.nsqre.insquare.Square;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.daimajia.swipe.SwipeLayout;
import com.nsqre.insquare.Activities.BottomNavActivity;
import com.nsqre.insquare.Activities.ChatActivity;
import com.nsqre.insquare.Fragments.MainContent.MapFragment;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.REST.VolleyManager;

import java.util.ArrayList;

/**
 * Created by Umberto Sonnino on 29/03/2016.
 */

/**
 * unused
 */
public class RecyclerSwipeSquareAdapter extends RecyclerView.Adapter {

    private static final String TAG = "SquareAdapter";
    public static final String NOTIFICATION_MAP = "NOTIFICATION_MAP";

    private Context context;
    public ArrayList<Square> squaresArrayList;
    int i = 0;

    public RecyclerSwipeSquareAdapter(Context c, ArrayList<Square> squares) {
        this.context = c;
        this.squaresArrayList = squares;
    }

    @Override
    public SquareViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.swipe_row_layout, parent, false);
        SquareViewHolder squareViewHolder = new SquareViewHolder(v);
        return squareViewHolder;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {

        final SquareViewHolder castHolder = (SquareViewHolder) holder;

        final Square listItem = this.squaresArrayList.get(position);

        setupHeart(castHolder, listItem);

        setupNotifications(castHolder, listItem);

        String squareName = listItem.getName();
        castHolder.squareName.setText(squareName);
        castHolder.squareActivity.setText(context.getString(R.string.square_last_message_incipit) + listItem.formatTime());

        setupLeftSection(castHolder, listItem.getInitials());

        setupSwipeLayout(castHolder, listItem);

        castHolder.swipeLayout.getSurfaceView().setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(MapFragment.SQUARE_TAG, listItem);
                        intent.putExtra(BottomNavActivity.INITIALS_TAG, castHolder.squareInitials.getText().toString());
                        int position = castHolder.getAdapterPosition() % (BottomNavActivity.backgroundColors.length);
                        intent.putExtra(BottomNavActivity.INITIALS_COLOR_TAG, BottomNavActivity.backgroundColors[position]);

                        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_MAP, Context.MODE_PRIVATE);
                        if (sharedPreferences.contains(listItem.getId())) {
                            sharedPreferences.edit().remove(listItem.getId()).apply();
                            sharedPreferences.edit().putInt("squareCount", sharedPreferences.getInt("squareCount", 0) - 1).apply();
                        }
//                        context.startActivity(intent);

                        // ====
                        BottomNavActivity madre = (BottomNavActivity) context;
                        Pair rowPair = new Pair<>(v.findViewById(R.id.cardview_square),
                                context.getString(R.string.transition_name_square_row));
                        Pair namePair = new Pair<>(v.findViewById(R.id.cardview_square_name),
                                context.getString(R.string.transition_name_square_name));
                        Pair initialsPair = new Pair<>(v.findViewById(R.id.cardview_left_section_circle),
                                context.getString(R.string.transition_name_square_circle));
                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                madre /*, namePair, initialsPair, rowPair*/

                        );

                        ActivityCompat.startActivity(madre, intent, options.toBundle());
                    }
                }
        );

        /*castHolder.itemView.setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (context instanceof BottomNavActivity) {
                            BottomNavActivity madre = (BottomNavActivity) context;
                            madre.showBottomSheetDialog(listItem, RecyclerSwipeSquareAdapter.this, castHolder.getAdapterPosition());
                        }
                        return true;
                    }
                }
        );*/
    }

    private void setupSwipeLayout(SquareViewHolder castHolder, Square square)
    {
        castHolder.swipeLayout.setShowMode(SwipeLayout.ShowMode.PullOut);
        
        castHolder.swipeLayout.addDrag(SwipeLayout.DragEdge.Right, castHolder.swipeLayout.findViewById(R.id.swipe_row_bottom_wrapper));

        castHolder.swipeLayout.addSwipeListener(
                new SwipeLayout.SwipeListener() {
                    @Override
                    public void onStartOpen(SwipeLayout layout) {

                    }

                    @Override
                    public void onOpen(SwipeLayout layout) {

                    }

                    @Override
                    public void onStartClose(SwipeLayout layout) {

                    }

                    @Override
                    public void onClose(SwipeLayout layout) {

                    }

                    @Override
                    public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

                    }

                    @Override
                    public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

                    }
                }
        );

        castHolder.bottomWrapper.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: I've just tried to delete something! =D");
                    }
                }
        );
    }

    private void setupLeftSection(SquareViewHolder castHolder, String initials) {
        int position = castHolder.getAdapterPosition()%(BottomNavActivity.backgroundColors.length);

        ColorStateList circleColor = ContextCompat.getColorStateList(context, BottomNavActivity.backgroundColors[position]);

        final Drawable originalDrawable = castHolder.squareCircle.getBackground();
        final Drawable wrappedDrawable = DrawableCompat.wrap(originalDrawable);
        DrawableCompat.setTintList(wrappedDrawable, circleColor);
        castHolder.squareCircle.setBackground(wrappedDrawable);
        castHolder.squareInitials.setText(initials);
    }

    private void setupHeart(final SquareViewHolder castHolder, final Square listItem) {
        if(InSquareProfile.isFav(listItem.getId())){
            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
        }else
        {
            castHolder.squareFav.setImageResource(R.drawable.heart_border_black);
        }

        castHolder.squareFav.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(InSquareProfile.isFav(listItem.getId()))
                        {
                            favouriteSquare(Request.Method.DELETE, listItem);
                            castHolder.squareFav.setImageResource(R.drawable.heart_border_black);
                        }else
                        {
                            favouriteSquare(Request.Method.POST, listItem);
                            castHolder.squareFav.setImageResource(R.drawable.like_filled_96);
                        }
                    }
                }
        );
    }

    public void removeElement(final int position)
    {
        final Square listItem = squaresArrayList.get(position);
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(context);

        builder.setTitle("Attenzione!")
                .setMessage("Tutti i messaggi associati a " + listItem.getName().toString().trim() + " andranno perduti.");
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String ownerId = InSquareProfile.getUserId();
                        final String squareId = listItem.getId();
                        VolleyManager.getInstance().deleteSquare(squareId, ownerId, new VolleyManager.VolleyResponseListener() {
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
                                // Lasciare vuoto
                            }

                            @Override
                            public void responseDELETE(Object object) {
                                boolean response = (boolean) object;
                                BottomNavActivity madre = (BottomNavActivity) context;

                                if (response) {
                                    Log.d(TAG, "responseDELETE: sono riuscito a eliminare correttamente!");
                                    Snackbar.make(madre.coordinatorLayout, "Cancellazione avvenuta con successo!", Snackbar.LENGTH_SHORT).show();
                                    squaresArrayList.remove(position);
                                    notifyItemRemoved(position);
                                } else {
                                    Log.d(TAG, "responseDELETE: c'e' stato un problema con la cancellazione");
                                    Snackbar.make(madre.coordinatorLayout, "C'e' stato un problema con la cancellazione!", Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                });
        builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        builder.create().show();

    }


    private void setupNotifications(SquareViewHolder castHolder, Square listItem) {

        SharedPreferences sharedPreferences = context.getSharedPreferences(NOTIFICATION_MAP, Context.MODE_PRIVATE);

        int squaresNewMessages = sharedPreferences.getInt(listItem.getId(), 0);
        if (squaresNewMessages == 0) {
            castHolder.squareNotifications.setVisibility(View.INVISIBLE);
        } else {
            castHolder.squareNotifications.setVisibility(View.VISIBLE);
        }
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return this.squaresArrayList.size();
    }

    private void favouriteSquare(final int method, final Square square) {

        VolleyManager.getInstance().handleFavoriteSquare(method, square.getId(), InSquareProfile.getUserId(),
                new VolleyManager.VolleyResponseListener() {
                    @Override
                    public void responseGET(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responsePOST(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responsePOST - non sono riuscito ad inserire il fav " + square.toString());
                        }else
                        {
                            InSquareProfile.addFav(square);
                        }
                    }

                    @Override
                    public void responsePATCH(Object object) {
                        // method e' POST o DELETE
                    }

                    @Override
                    public void responseDELETE(Object object) {
                        if(object == null)
                        {
                            //La richiesta e' fallita
                            Log.d(TAG, "responseDELETE - non sono riuscito ad rimuovere il fav " + square.toString());
                        }else
                        {
                            InSquareProfile.removeFav(square.getId());
                        }
                    }
                });
    }

    public static class SquareViewHolder extends RecyclerView.ViewHolder {

        SwipeLayout swipeLayout;
        TextView squareInitials;
        public TextView squareName;
        TextView squareActivity;
        ImageView squareNotifications;
        ImageView squareFav;
        ImageView squareCircle;

        LinearLayout bottomWrapper;

        public SquareViewHolder(View itemView) {
            super(itemView);

            swipeLayout = (SwipeLayout) itemView.findViewById(R.id.swipe_row_layout);

            squareName = (TextView) itemView.findViewById(R.id.swipe_row_upper_square_name);
            squareActivity = (TextView) itemView.findViewById(R.id.swipe_row_upper_square_last_activity);
            squareNotifications = (ImageView) itemView.findViewById(R.id.swipe_row_upper_square_notification_counter);
            squareCircle = (ImageView) itemView.findViewById(R.id.swipe_row_upper_left_section_circle);
            squareInitials = (TextView) itemView.findViewById(R.id.swipe_row_upper_square_initials);

            squareFav = (ImageView) itemView.findViewById(R.id.swipe_row_upper_square_heart);

            bottomWrapper = (LinearLayout) itemView.findViewById(R.id.swipe_row_bottom_wrapper);

        }

    }
}
