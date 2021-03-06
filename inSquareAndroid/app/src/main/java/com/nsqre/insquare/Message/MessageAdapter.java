package com.nsqre.insquare.Message;/* Created by umbertosonnino on 2/1/16  */

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.leocardz.link.preview.library.LinkPreviewCallback;
import com.leocardz.link.preview.library.SourceContent;
import com.leocardz.link.preview.library.TextCrawler;
import com.nsqre.insquare.Activities.FullScreenImageActivity;
import com.nsqre.insquare.R;
import com.nsqre.insquare.User.InSquareProfile;
import com.nsqre.insquare.Utilities.MyLeadingMarginSpan2;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.regex.Matcher;

/**
 * The adapter for the message list that the user views in the ChatActivity
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageHolder>
{
    private static final String TAG = "MessageAdapter";
    private ArrayList<Message> mDataset;
    private static ChatMessageClickListener myClickListener;
    private Context context;
    private TextCrawler textCrawler;
    private LinkedList<Integer> urlPositionsQueue;

    public MessageAdapter(Context c)
    {
        this.context = c;
        this.mDataset = new ArrayList<>();
        textCrawler = new TextCrawler();
        urlPositionsQueue = new LinkedList<>();
    }

    /**
     * Returns the view type of the message in a certain position
     * 0 for text message from someone else, 1 for text message from me
     * 2 for photo from someone else, 3 for photo from me
     * 4 for outgoing photo, 5 for outgoing text message
     * @param position the position on the view of the message
     * @return the type of the message
     */
    @Override
    public int getItemViewType(int position) {
        Message m = mDataset.get(position);

        if (m.getFrom().equals(InSquareProfile.getUserId())) {
            if (m.getText().contains("http://i.imgur.com/") && isOutgoing(m)) {
                return 4;
            } else if (m.getText().contains("http://i.imgur.com/")) {
                return 3;
            }
            if (isOutgoing(m))
                return 5;
            else
                return 1;
        } else if (m.getText().contains("http://i.imgur.com/")) {
            return 2;
        }
        return 0;
    }

    /**
     * Inflates the right layout for the kind of message it has to show. Calls getItemViewType to know it
     * @return a message holder
     * @see #getItemViewType(int)
     */
    @Override
    public MessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType)
        {
            case 0:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
                break;
            case 1:
            case 5:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item_me, parent, false);
                break;
            case 2:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.foto_item, parent, false);
                break;
            case 3:
            case 4:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.foto_item_me, parent, false);
                break;

        }
        MessageHolder msgHld = new MessageHolder(view, viewType);  //va a 3
        return msgHld;  //dopo aver creato il msgHld va su 4
    }

    /**
     * Draws on the screen the correct layout for the file
     * @param holder the message to show
     * @param position position of the message on the list
     */
    @Override
    public void onBindViewHolder(final MessageHolder holder, int position) {
        final Message m = mDataset.get(position);
        int type = getItemViewType(position);
        Transformation transformation = new Transformation() {
            @Override
            public Bitmap transform(Bitmap source) {
                int targetWidth = holder.foto.getWidth();

                double aspectRatio = (double) source.getHeight() / (double) source.getWidth();
                int targetHeight = (int) (targetWidth * aspectRatio);
                Bitmap result = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false);
                if (result != source) {
                    // Same bitmap is returned if sizes are the same
                    source.recycle();
                }
                return result;
            }

            @Override
            public String key() {
                return "transformation" + " desiredWidth";
            }
        };

        switch (type)
        {
            case 0: {
                holder.content.setText(m.getText());
                holder.username.setText(m.getName());
                break;
            }
            case 1:
            case 5: {
                holder.content.setText(m.getText());
                break;
            }
            case 2: {
                holder.username.setText(m.getName());
                Picasso.with(context)
                        .load(m.getText())
                        .placeholder(R.drawable.ic_photo_library_black)
                        .transform(transformation)
                        .into(holder.foto);
                holder.foto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String photoURL = m.getText();

                        Intent fullScreenIntent = new Intent(v.getContext(), FullScreenImageActivity.class);
                        fullScreenIntent.putExtra("photoURL", photoURL);

                        v.getContext().startActivity(fullScreenIntent);
                    }
                });
                break;
            }
            case 3:
            case 4: {
                Picasso.with(context)
                        .load(m.getText())
                        .placeholder(R.drawable.ic_photo_library_black)
                        .transform(transformation)
                        .into(holder.foto);
                holder.foto.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String photoURL = m.getText();

                        Intent fullScreenIntent = new Intent(v.getContext(), FullScreenImageActivity.class);
                        fullScreenIntent.putExtra("photoURL", photoURL);

                        v.getContext().startActivity(fullScreenIntent);
                    }
                });
                break;
            }
        }

        String timetoShow = "";
        Calendar c = Calendar.getInstance();
        int tYear = c.get(Calendar.YEAR);
        int tDay = c.get(Calendar.DAY_OF_MONTH);

        Calendar msgCal = m.getCalendar();
        int mYear = msgCal.get(Calendar.YEAR);
        int mDay = msgCal.get(Calendar.DAY_OF_MONTH);

        Locale l = this.context.getResources().getConfiguration().locale;
        DateFormat df;
        if(mYear != tYear)
        {
            df = new SimpleDateFormat("MMM d, ''yy, HH:mm", l);
        }else if(mDay != tDay)
        {
            df = new SimpleDateFormat("MMM d, HH:mm", l);
        }else
        {
            df = new SimpleDateFormat("HH:mm", l);
        }

        timetoShow = df.format(msgCal.getTime());

//        Log.d(TAG, "onBindViewHolder: calendar is " + mYear + " " + mDay);
        holder.datetime.setText(timetoShow);
        if(m.getUrlProvider() != null && !"".equals(m.getUrlProvider())) {
            holder.urlProvider.setText(m.getUrlProvider());
            holder.urlProvider.setVisibility(View.VISIBLE);
        } else if(holder.urlProvider != null) {
            holder.urlProvider.setText("provider");
            holder.urlProvider.setVisibility(View.GONE);
        }
        if(m.getUrlTitle() != null && !"".equals(m.getUrlTitle())) {
            holder.urlTitle.setText(m.getUrlTitle());
            holder.urlTitle.setVisibility(View.VISIBLE);
        } else if(holder.urlTitle!= null) {
            holder.urlTitle.setText("Title");
            holder.urlTitle.setVisibility(View.GONE);
        }
        if(m.getUrlImage() != null && !"".equals(m.getUrlImage())) {
            Picasso.with(context)
                    .load(m.getUrlImage())
                    .resize(dpToPx(50),dpToPx(50))
                    .centerCrop()
                    .into(holder.urlImage);
            holder.urlImage.setVisibility(View.VISIBLE);
        } else if(holder.urlImage != null) {
            holder.urlImage.setVisibility(View.GONE);
        }
        if(m.getUrlDescription() != null && !"".equals(m.getUrlProvider())) {
            if(m.getUrlImage() != null && !"".equals(m.getUrlImage())) {
                SpannableStringBuilder spannableString = new SpannableStringBuilder(m.getUrlDescription());
                spannableString.setSpan(new MyLeadingMarginSpan2(3, dpToPx(60)),
                        0, spannableString.length(), 0);
                holder.urlDescription.setText(spannableString);
            } else {
                holder.urlDescription.setText(m.getUrlDescription());
            }
            holder.urlDescription.setVisibility(View.VISIBLE);
        } else if(holder.urlDescription != null) {
            holder.urlDescription.setText("description");
            holder.urlDescription.setVisibility(View.GONE);
        }
        if(m.isLineVisible()) {
            holder.urlLine.setVisibility(View.VISIBLE);
        } else if(holder.urlLine != null) {
            holder.urlLine.setVisibility(View.GONE);
        }
    }

    /**
     * Calculates the number of pixels given a number of dp
     * @param dp the dp to convert
     * @return the conversion of dp into pixels
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * Method that creates a link preview based on the given url
     * @param message that contains the url
     * @param position of the item in the adapter
     */
    private void checkUrl(final Message message, int position) {
        Matcher m = Patterns.WEB_URL.matcher(message.getText());
        if(m.find() && message.getUrlProvider() == null
                && message.getUrlTitle() == null
                && message.getUrlDescription() == null) {
            String url = m.group();
            if(m.group().contains("http://i.imgur.com/")) {
                return;
            }
            urlPositionsQueue.add(position);
            Log.d("checkUrl", "URL extracted: " + url);
            textCrawler.makePreview(new LinkPreviewCallback() {
                @Override
                public void onPre() {

                }

                @Override
                public void onPos(SourceContent sourceContent, boolean isNull) {
                    if (!isNull && !sourceContent.getFinalUrl().equals("")) {
                        Log.d("checkUrl", sourceContent.getCannonicalUrl() + " " + sourceContent.getTitle() +
                                " " + sourceContent.getDescription());
                        Message m = mDataset.get(urlPositionsQueue.getFirst());
                        m.setUrlProvider(sourceContent.getCannonicalUrl());
                        m.setUrlTitle(sourceContent.getTitle());
                        m.setUrlDescription(sourceContent.getDescription());
                        if(sourceContent.getImages().size() > 0) {
                            m.setUrlImage(sourceContent.getImages().get(0));
                        }
                        m.setIsLineVisible(true);
                        notifyItemChanged(urlPositionsQueue.getFirst());
                        urlPositionsQueue.remove(0);
                    }
                }
            }, url);
        }
    }

    /**
     * Adds an item to the dataset
     * @param msg the message to add
     * @see #checkUrl(Message, int)
     */
    public void addItem(Message msg) {
        mDataset.add(msg);
        checkUrl(msg,mDataset.size()-1);
        notifyItemInserted(mDataset.size() - 1);
    }

    public Message getMessage(int position)
    {
        return this.mDataset.get(position);
    }

    public Message getMessage(Message message) {
        return getMessage(this.mDataset.indexOf(message));
    }

    public void removeItem(int position) {
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

    public void clear() {
        for(int i = 0; i<mDataset.size(); i++) {
            removeItem(i);
        }
    }

    public boolean contains(Message msg) {
        return mDataset.contains(msg);
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setOnClickListener(ChatMessageClickListener clickListener)
    {
        this.myClickListener = clickListener;
    }

    /**
     * Checks if the app is still trying to send that message
     * @param m the message to check
     * @return true if the message is still not sent
     */
    private boolean isOutgoing(Message m) {
        InSquareProfile mProfile = InSquareProfile.getInstance(context);
        for (ArrayList<Message> arr : mProfile.getOutgoingMessages().values()) {
            for (Message message : arr) {
                if (message == m)
                    return true;
            }
        }
        return false;
    }

    /**
     * Contains relevant data on how to draw messages on the screen
     */
    public static class MessageHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView content;
        private ImageView foto;
        private TextView username;
        private TextView datetime;
        private RelativeLayout urlPreview;
        private TextView urlProvider;
        private TextView urlTitle;
        private TextView urlDescription;
        private View urlLine;
        private ImageView urlImage;
        private RelativeLayout relativeLayout;
        private ImageView outgoingIcon;

        //3: si prende questi dati
        public MessageHolder(View itemView, int viewType) {
            super(itemView);
            foto = (ImageView) itemView.findViewById((R.id.foto_content));
            content = (TextView) itemView.findViewById(R.id.message_content);
            username = (TextView) itemView.findViewById(R.id.message_sender);
            datetime = (TextView) itemView.findViewById(R.id.message_timestamp);
            urlPreview = (RelativeLayout) itemView.findViewById(R.id.url_preview);
            urlLine = itemView.findViewById(R.id.url_line);
            urlProvider = (TextView) itemView.findViewById(R.id.url_provider);
            urlTitle = (TextView) itemView.findViewById(R.id.url_title);
            urlDescription = (TextView) itemView.findViewById(R.id.url_description);
            urlImage = (ImageView) itemView.findViewById(R.id.url_image);
            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.message_relative_layout);
            outgoingIcon = (ImageView) itemView.findViewById(R.id.message_outgoing_icon);

            if (viewType == 4 || viewType == 5) {
                datetime.setVisibility(View.INVISIBLE);
                outgoingIcon.setVisibility(View.VISIBLE);
            }
            else if (viewType == 1 || viewType == 3){
                datetime.setVisibility(View.VISIBLE);
                outgoingIcon.setVisibility(View.INVISIBLE);
            }

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(), v);
        }

    }

    public interface ChatMessageClickListener {
        public void onItemClick(int position, View v);
    }
}
