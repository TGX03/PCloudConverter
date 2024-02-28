package eu.tgx03.pcloud;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

/**
 * A small tool which converts u.pcloud.link URLs to the actual file link, so it can be automatically downloaded in a download manager.
 * When dealing with video files, it automatically chooses the one with the highest quality.
 */
public class PCloudConverter {

	/**
	 * Queries the given url and returns the actual file URL on pcloud's servers.
	 *
	 * @param url    The URL to query.
	 * @param region If multiple locations offer this file, this allows to set a preferred region.
	 * @return If the connection could not be made.
	 * @throws IOException              If something went wrong during the connection to PCloud.
	 * @throws IllegalArgumentException If no valid pcloud-link url was provided.
	 */
	public static String convertURL(@NotNull String url, Region region) throws IOException, IllegalArgumentException {
		if (!url.startsWith("https://u.pcloud.link/publink/show?code="))
			throw new IllegalArgumentException("Not provided a valid PCloud-link URL");
		JSONArray array = findVariants(queryURL(url));
		return createLink(array, region);
	}

	/**
	 * Queries the URL and simply returns the full HTML of the page.
	 *
	 * @param query The URL of the page to query.
	 * @return A string of the full HTML.
	 * @throws IOException If something went wrong during network communication.
	 */
	@NotNull
	private static String queryURL(@NotNull String query) throws IOException {
		URL url = new URL(query);
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				builder.append(line).append(System.lineSeparator());
			}
			return builder.toString();
		}
	}

	/**
	 * Parses the page for the actual links and puts all of them in a JSONArray
	 * This only results in multiple variants for video files as far as I know.
	 *
	 * @param page The HTML to parse.
	 * @return The found variants for the file.
	 */
	 @NotNull
	private static JSONArray findVariants(@NotNull String page) {
		String[] strings = page.stripIndent().split("\n");
		StringBuilder variants = new StringBuilder();
		variants.append('[');
		boolean started = false;
		long depth = 0;
		for (String string : strings) {
			if (started) {
				depth = depth + string.codePoints().filter(x -> x == '[').count();
				depth = depth - string.codePoints().filter(x -> x == ']').count();
				if (depth >= 0) variants.append(string).append(System.lineSeparator());
				else break;
			}
			if (string.contains("\"variants\": [")) started = true;
		}
		variants.append(']');
		return new JSONArray(variants.toString());
	}

	/**
	 * Create the final link from all the variants found.
	 *
	 * @param array  The array of all the variants found for the file.
	 * @param region The region to prefer if multiple are available.
	 * @return The created link.
	 */
	@NotNull
	private static String createLink(@NotNull JSONArray array, @NotNull Region region) {
		Movie[] movies = new Movie[array.length()];
		for (int i = 0; i < array.length(); i++) {
			JSONObject object = array.getJSONObject(i);
			JSONArray hostArray = object.getJSONArray("hosts");
			String[] hosts = new String[hostArray.length()];
			for (int j = 0; j < hostArray.length(); j++) {
				hosts[j] = hostArray.getString(j);
			}
			String host = region.getHost(hosts);
			long bitrate = -1;
			try {
				bitrate = object.getLong("videobitrate");
			} catch (JSONException ignored) {
			}  // Gets thrown when not a mp4-file.
			Movie movie = new Movie(object.getString("path"), bitrate, host);
			movies[i] = movie;
		}
		Arrays.sort(movies);
		return movies[movies.length - 1].host() + movies[movies.length - 1].path();
	}

	/**
	 * Allows the setting of a preferred region for creating links.
	 * Currently, 2 regions are known and supported.
	 */
	public enum Region {
		EUROPE("p-lux"),
		AMERICA("p-def");

		/**
		 * The prefix of the CDN for the corresponding region.
		 */
		private final String prefix;

		Region(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * Returns the first link from the given array of links that matches this region.
		 *
		 * @param hosts All the hosts for a given file.
		 * @return The one from this region.
		 */
		@Nullable
		private String getHost(@Nullable String @NotNull [] hosts) {
			for (String host : hosts) {
				if (host != null) {
					assert host.matches("p-def\\d+.pcloud.com") || host.matches("p-lux\\d+.pcloud.com");
					if (host.startsWith(this.prefix)) return host;
				}
			}
			return hosts[0];
		}
	}
}
