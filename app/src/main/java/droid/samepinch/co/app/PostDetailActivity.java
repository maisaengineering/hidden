///*
// * Copyright (C) 2015 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//    private void updateBackground(FloatingActionButton fab, Palette palette) {
//        int lightVibrantColor = palette.getLightVibrantColor(getResources().getColor(android.R.color.white));
//        int vibrantColor = palette.getVibrantColor(getResources().getColor(R.color.accent_material_dark));
//
//        fab.setRippleColor(lightVibrantColor);
//        fab.setBackgroundTintList(ColorStateList.valueOf(vibrantColor));
//    }
//}
//

/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package droid.samepinch.co.app;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import droid.samepinch.co.app.helpers.Utils;
import droid.samepinch.co.data.dao.SchemaPosts;
import droid.samepinch.co.data.dto.Post;

public class PostDetailActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postdetail);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get the ActionBar here to configure the way it behaves.
        final ActionBar ab = getSupportActionBar();
        //ab.setHomeAsUpIndicator(R.drawable.ic_menu); // set a custom icon for the default home button
        ab.setDisplayShowHomeEnabled(true); // show or hide the default home button
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true); // enable overriding the default toolbar layout
        ab.setDisplayShowTitleEnabled(false);

        LinearLayout contentLayout = (LinearLayout) findViewById(R.id.postdetail_content);
        ViewGroup.LayoutParams lParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // get caller data
        Bundle iArgs = getIntent().getExtras();
        String postId = iArgs.getString(SchemaPosts.COLUMN_UID);

        Post post = null;
        Cursor cursor = getContentResolver().query(SchemaPosts.CONTENT_URI, null, SchemaPosts.COLUMN_UID + "=?", new String[]{postId}, null);
        if(cursor != null && cursor.moveToFirst()){
            post = Utils.cursorToPostEntity(cursor);
        }

        TextView textView, recentView;
        for (int i = 0; i < 5; i++) {
            //Create a textView, set a random ID and position it below the most recently added view
            textView = new TextView(PostDetailActivity.this);
            textView.setId((int) System.currentTimeMillis());
            textView.setText(post.getContent());
            contentLayout.addView(textView, lParams);
            recentView = textView;
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadBackdrop() {
        final ImageView imageView = (ImageView) findViewById(R.id.backdrop);
        //Glide.with(this).load(Cheeses.getRandomCheeseDrawable()).centerCrop().into(imageView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sample_actions, menu);
        return true;
    }
}