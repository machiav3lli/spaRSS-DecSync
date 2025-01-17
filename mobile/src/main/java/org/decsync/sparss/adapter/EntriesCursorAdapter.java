/**
 * spaRSS
 * <p/>
 * Copyright (c) 2015-2016 Arnaud Renaud-Goud
 * Copyright (c) 2012-2015 Frederic Julian
 * <p/>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * <p/>
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 * <p/>
 * Copyright (c) 2010-2012 Stefan Handschuh
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.decsync.sparss.adapter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.squareup.picasso.Picasso;

import org.decsync.sparss.Constants;
import org.decsync.sparss.MainApplication;
import org.decsync.sparss.R;
import org.decsync.sparss.provider.FeedData;
import org.decsync.sparss.provider.FeedData.EntryColumns;
import org.decsync.sparss.provider.FeedData.FeedColumns;
import org.decsync.sparss.utils.CircleTransform;
import org.decsync.sparss.utils.DB;
import org.decsync.sparss.utils.NetworkUtils;
import org.decsync.sparss.utils.PrefUtils;
import org.decsync.sparss.utils.StringUtils;

public class EntriesCursorAdapter extends ResourceCursorAdapter {

    private final Uri mUri;
    private final boolean mShowFeedInfo;
    private final CircleTransform mCircleTransform = new CircleTransform();
    private int mIdPos, mTitlePos, mMainImgPos, mDatePos, mAuthorPos, mIsReadPos, mFavoritePos, mFeedIdPos, mFeedIconPos, mFeedNamePos;

    public EntriesCursorAdapter(Context context, Uri uri, Cursor cursor, boolean showFeedInfo) {
        super(context, R.layout.item_entry_list, cursor, 0);
        mUri = uri;
        mShowFeedInfo = showFeedInfo;

        reinit(cursor);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        if (view.getTag(R.id.holder) == null) {
            ViewHolder holder = new ViewHolder();
            holder.titleTextView = (TextView) view.findViewById(R.id.titleText);
            holder.sourceAuthorTextView = (TextView) view.findViewById(R.id.sourceAuthorText);
            holder.dateTimeTextView = (TextView) view.findViewById(R.id.dateTimeText);
            holder.mainImgView = (ImageView) view.findViewById(R.id.main_icon);
            holder.starImgView = (ImageView) view.findViewById(R.id.favorite_icon);
            view.setTag(R.id.holder, holder);
        }

        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        String titleText = cursor.getString(mTitlePos);
        holder.titleTextView.setText(titleText);

        holder.dateTimeTextView.setText(StringUtils.getDateTimeString(cursor.getLong(mDatePos)));

        final long feedId = cursor.getLong(mFeedIdPos);
        String feedName = cursor.getString(mFeedNamePos);

        String mainImgUrl = cursor.getString(mMainImgPos);
        mainImgUrl = TextUtils.isEmpty(mainImgUrl) ? null : NetworkUtils.getDownloadedOrDistantImageUrl(cursor.getLong(mIdPos), mainImgUrl);

        ColorGenerator generator = ColorGenerator.DEFAULT;
        int color = generator.getColor(Long.valueOf(feedId)); // The color is specific to the feedId (which shouldn't change)
        TextDrawable letterDrawable = TextDrawable.builder().buildRound((feedName != null ? feedName.substring(0, 1).toUpperCase() : ""), color);
        if (mainImgUrl != null && PrefUtils.getBoolean(PrefUtils.DISPLAY_IMAGES, true)) {
            Picasso.with(context).load(mainImgUrl).transform(mCircleTransform).placeholder(letterDrawable).error(letterDrawable).into(holder.mainImgView);
        } else {
            Picasso.with(context).cancelRequest(holder.mainImgView);
            holder.mainImgView.setImageDrawable(letterDrawable);
        }

        holder.isFavorite = cursor.getInt(mFavoritePos) == 1;

        holder.starImgView.setVisibility(holder.isFavorite ? View.VISIBLE : View.INVISIBLE);

        String authorText = cursor.getString(mAuthorPos);
        if (authorText == null || authorText.isEmpty()) {
            holder.sourceAuthorTextView.setVisibility(View.INVISIBLE);
        } else {
            if (mShowFeedInfo && mFeedNamePos > -1 && feedName != null) {
                holder.sourceAuthorTextView.setText(Html.fromHtml(new StringBuilder("<font color='#247ab0'>").append(feedName).append("</font>").append(Constants.COMMA_SPACE).append(authorText).toString()));
            } else {
                holder.sourceAuthorTextView.setText(authorText);
            }
        }

        if (cursor.isNull(mIsReadPos)) {
            holder.titleTextView.setEnabled(true);
            holder.sourceAuthorTextView.setEnabled(true);
            holder.dateTimeTextView.setEnabled(true);
            holder.isRead = false;
        } else {
            holder.titleTextView.setEnabled(false);
            holder.sourceAuthorTextView.setEnabled(false);
            holder.dateTimeTextView.setEnabled(false);
            holder.isRead = true;
        }
    }

    public void toggleReadState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isRead = !holder.isRead;

            if (holder.isRead) {
                holder.titleTextView.setEnabled(false);
                holder.sourceAuthorTextView.setEnabled(false);
                holder.dateTimeTextView.setEnabled(false);
            } else {
                holder.titleTextView.setEnabled(true);
                holder.sourceAuthorTextView.setEnabled(true);
                holder.dateTimeTextView.setEnabled(true);
            }

            new Thread() {
                @Override
                public void run() {
                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    Context context = MainApplication.getContext();
                    DB.update(context, entryUri, holder.isRead ? FeedData.getReadContentValues() : FeedData.getUnreadContentValues(), null, null);
                }
            }.start();
        }
    }

    public void toggleFavoriteState(final long id, View view) {
        final ViewHolder holder = (ViewHolder) view.getTag(R.id.holder);

        if (holder != null) { // should not happen, but I had a crash with this on PlayStore...
            holder.isFavorite = !holder.isFavorite;

            if (holder.isFavorite) {
                holder.starImgView.setVisibility(View.VISIBLE);
            } else {
                holder.starImgView.setVisibility(View.INVISIBLE);
            }

            new Thread() {
                @Override
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(EntryColumns.IS_FAVORITE, holder.isFavorite ? 1 : 0);

                    Uri entryUri = ContentUris.withAppendedId(mUri, id);
                    Context context = MainApplication.getContext();
                    DB.update(context, entryUri, values, null, null);
                }
            }.start();
        }
    }

    public void markAllAsRead(final long untilDate) {
        new Thread() {
            @Override
            public void run() {
                String where = EntryColumns.WHERE_UNREAD + Constants.DB_AND + '(' + EntryColumns.FETCH_DATE + Constants.DB_IS_NULL + Constants.DB_OR + EntryColumns.FETCH_DATE + "<=" + untilDate + ')';
                Context context = MainApplication.getContext();
                DB.update(context, mUri, FeedData.getReadContentValues(), where, null);
            }
        }.start();
    }

    @Override
    public void changeCursor(Cursor cursor) {
        reinit(cursor);
        super.changeCursor(cursor);
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        reinit(newCursor);
        return super.swapCursor(newCursor);
    }

    @Override
    public void notifyDataSetChanged() {
        reinit(null);
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        reinit(null);
        super.notifyDataSetInvalidated();
    }

    private void reinit(Cursor cursor) {
        if (cursor != null && cursor.getCount() > 0) {
            mIdPos = cursor.getColumnIndex(EntryColumns._ID);
            mTitlePos = cursor.getColumnIndex(EntryColumns.TITLE);
            mMainImgPos = cursor.getColumnIndex(EntryColumns.IMAGE_URL);
            mDatePos = cursor.getColumnIndex(EntryColumns.DATE);
            mAuthorPos = cursor.getColumnIndex(EntryColumns.AUTHOR);
            mIsReadPos = cursor.getColumnIndex(EntryColumns.IS_READ);
            mFavoritePos = cursor.getColumnIndex(EntryColumns.IS_FAVORITE);
            mFeedNamePos = cursor.getColumnIndex(FeedColumns.NAME);
            mFeedIdPos = cursor.getColumnIndex(EntryColumns.FEED_ID);
            mFeedIconPos = cursor.getColumnIndex(FeedColumns.ICON);
        }
    }

    private static class ViewHolder {
        public TextView titleTextView;
        public TextView sourceAuthorTextView;
        public TextView dateTimeTextView;
        public ImageView mainImgView;
        public ImageView starImgView;
        public boolean isRead, isFavorite;
    }
}
