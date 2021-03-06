package app.com.example.android.popular_movies_p1;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by shavant on 2/13/16.
 */
public class FetchMovieDataTask extends AsyncTask<String, Void, ArrayList> {
    protected ArrayList<MovieGson> movieGsonArrayList;
    protected static ImageListAdapter imgListAdapter;
    private Context context;

    public FetchMovieDataTask(Context context) {
        this.context = context;
    }

    private final String LOG_TAG = FetchMovieDataTask.class.getSimpleName();
    private final String baseImgPath = "http://image.tmdb.org/t/p/";

    // List of JSON objects
    final String TMDB_RESULTS = "results";
    final String RELATIVE_IMG_PATH = "poster_path";

    private ArrayList<String> createFromJson(String movieJsonStr)
            throws JSONException {
        Gson gson = new Gson();

        // JSON Objects, Arrays and ArrayList
        JSONObject moviesJson = new JSONObject(movieJsonStr);
        JSONArray moviesArray = moviesJson.getJSONArray(TMDB_RESULTS);
        movieGsonArrayList = new ArrayList<MovieGson>(moviesArray.length());
        ArrayList<String> imgPathList = new ArrayList<String>(moviesArray.length());

            /* Clear out any old instances of the Movie Array list and instantiate
               with length of JsonArray */
        if (!movieGsonArrayList.isEmpty()) {movieGsonArrayList.clear();}


        for (int i=0; i < moviesArray.length(); i++) {
            JSONObject movieJson = moviesArray.getJSONObject(i);
            // Lets take the short path info and create a full path
            String part_temp_path = movieJson.getString(RELATIVE_IMG_PATH);
            // Build real path for poster image and replace "poster_path" data
            String pathResult = buildPosterPath(part_temp_path);
            movieJson.remove(RELATIVE_IMG_PATH);
            movieJson.put(RELATIVE_IMG_PATH, pathResult);

            // JSON to Gson conversion for MovieGson Class and add to Movie Array List
            MovieGson movieGToGSon = gson.fromJson(movieJson.toString(), MovieGson.class);
            movieGsonArrayList.add(movieGToGSon);
            // Store the image path
            imgPathList.add(movieGToGSon.getPoster_path());
        }
        return imgPathList;
    }

    private String buildPosterPath(String relativePath) {
        final String IMG_WIDTH = "w185";
        Uri buildImgPath = Uri.parse(baseImgPath).buildUpon()
                .appendPath(IMG_WIDTH)
                . build();
        String full_path = buildImgPath.toString() + relativePath;
        return full_path;
    }

    @Override
    protected void onPostExecute(ArrayList result) {
        super.onPostExecute(result);
        if (imgListAdapter != null) {
            imgListAdapter.clear();

            // Retrieve and store the json data containing the Movie Details needed.
            for (int i = 0; i < movieGsonArrayList.size(); i++) {
                try {
                    // Poster Path Data
                    MovieGson movieGson = movieGsonArrayList.get(i);
                    imgListAdapter.add(movieGson.getPoster_path());
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            imgListAdapter.notifyDataSetChanged();
        }
    }

    protected ArrayList<String> doInBackground(String... params) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String rawJsonStr = null;
        String sortBy  = params[0];

        //Log.v(LOG_TAG, "Sort By Method is " + sortBy);

        try {
            final String MOVIE_API_BASEURL = "https://api.themoviedb.org/3";
            final String DISCOVER = "discover";
            final String SORT = "sort_by";
            final String API_KEY = "api_key";

            // Build URI to create a URL Web address
            Uri uriBuild = Uri.parse(MOVIE_API_BASEURL).buildUpon()
                    .appendPath(DISCOVER)
                    .appendPath("movie")
                    .appendQueryParameter(SORT, sortBy)
                    .appendQueryParameter(API_KEY, BuildConfig.THE_MOVIE_DB_API_KEY)
                    .build();
            URL url = new URL(uriBuild.toString());
            //Log.v(LOG_TAG, "URL " + url);

            // Make a connection to the movie DB
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            rawJsonStr = buffer.toString();


            try {
                ArrayList<String> posters = createFromJson(rawJsonStr);
                return posters;
            }
            catch (JSONException e) {
                Log.e(LOG_TAG, "Error " + e.getMessage(), e);
                e.printStackTrace();
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the data, there's no point in attemping
            // to parse it.
            return null;
        }
        finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }

        }

        return null;
    }

}
