package eu.tgx03.pcloud;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a file on pcloud. Is called Movie because the issue of not being downloadable mainly exist for movie files.
 *
 * @param path    The path on pcloud's servers for this file.
 * @param bitrate The bitrate of this file if available.
 * @param host    The host for this file.
 */
record Movie(@NotNull String path, long bitrate, @NotNull String host) implements Comparable<Movie> {

	@Override
	public int compareTo(Movie o) {
		return Long.compare(this.bitrate, o.bitrate);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Movie m) {
			return this.path.equals(m.path);
		} else return false;
	}

	public int hashCode() {
		return this.path.hashCode();
	}

	@NotNull
	public String toString() {
		return this.path;
	}
}
