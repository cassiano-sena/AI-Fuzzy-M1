import java.io.*;
import java.util.*;

public class FuzzyMovieRanking {

    static class Movie {
        String index;
        String budget;
        String genres;
        String homepage;
        String id;
        String keywords;
        String originalLanguage;
        String originalTitle;
        String overview;
        String popularity;
        String productionCompanies;
        String productionCountries;
        String releaseDate;
        String revenue;
        int runtime;
        String spokenLanguages;
        String status;
        String tagline;
        String title;
        double voteAverage;
        String voteCount;
        String cast;
        String crew;
        String director;

        Movie(String index, String budget, String genres, String homepage, String id, String keywords,
              String originalLanguage, String originalTitle, String overview, String popularity,
              String productionCompanies, String productionCountries, String releaseDate, String revenue,
              int runtime, String spokenLanguages, String status, String tagline, String title,
              double voteAverage, String voteCount, String cast, String crew, String director) {
            this.index = index;
            this.budget = budget;
            this.genres = genres;
            this.homepage = homepage;
            this.id = id;
            this.keywords = keywords;
            this.originalLanguage = originalLanguage;
            this.originalTitle = originalTitle;
            this.overview = overview;
            this.popularity = popularity;
            this.productionCompanies = productionCompanies;
            this.productionCountries = productionCountries;
            this.releaseDate = releaseDate;
            this.revenue = revenue;
            this.runtime = runtime;
            this.spokenLanguages = spokenLanguages;
            this.status = status;
            this.tagline = tagline;
            this.title = title;
            this.voteAverage = voteAverage;
            this.voteCount = voteCount;
            this.cast = cast;
            this.crew = crew;
            this.director = director;
        }
    }

    static class UserPreferences {
        String preferredGenre;
        double minRating;
        int maxRuntime;
    }

    public static List<Movie> loadMovies(String filename) {
        List<Movie> movies = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 24) continue;

                String index = tokens[0];
                String budget = tokens[1];
                String genres = tokens[2];
                String homepage = tokens[3];
                String id = tokens[4];
                String keywords = tokens[5];
                String originalLanguage = tokens[6];
                String originalTitle = tokens[7];
                String overview = tokens[8];
                String popularity = tokens[9];
                String productionCompanies = tokens[10];
                String productionCountries = tokens[11];
                String releaseDate = tokens[12];
                String revenue = tokens[13];

                int runtime = 0;
                try {
                    runtime = Integer.parseInt(tokens[14]);
                } catch (NumberFormatException e) {
                    // skip
                }

                String spokenLanguages = tokens[15];
                String status = tokens[16];
                String tagline = tokens[17];
                String title = tokens[18];

                double voteAverage = 0.0;
                try {
                    voteAverage = Double.parseDouble(tokens[19]);
                } catch (NumberFormatException e) {
                    // skip
                }

                String voteCount = tokens[20];
                String cast = tokens[21];
                String crew = tokens[22];
                String director = tokens[23];

                Movie movie = new Movie(
                        index, budget, genres, homepage, id, keywords, originalLanguage, originalTitle,
                        overview, popularity, productionCompanies, productionCountries, releaseDate, revenue,
                        runtime, spokenLanguages, status, tagline, title, voteAverage, voteCount, cast, crew, director
                );
                movies.add(movie);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
            e.printStackTrace();
        }
        return movies;
    }

    // need to improve this function, not fuzzy enough
    public static double calculateScore(Movie movie, UserPreferences prefs) {
        double score = 0.0;
        if (movie.genres != null && !movie.genres.isBlank()) {
            if (movie.genres.toLowerCase().contains(prefs.preferredGenre.toLowerCase())) {
                score += 5.0;
            }
        }
        if (movie.voteAverage >= prefs.minRating) {
            score += (movie.voteAverage - prefs.minRating) * 2.0;
        }
        if (movie.runtime <= prefs.maxRuntime) {
            score += 3.0;
        }
        return score;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserPreferences prefs = new UserPreferences();

        System.out.print("Enter your preferred genre: ");
        prefs.preferredGenre = scanner.nextLine().trim();

        System.out.print("Enter minimum vote average rating (e.g., 5.0): ");
        while (!scanner.hasNextDouble()) {
            System.out.print("Please enter a valid number for min rating: ");
            scanner.next();
        }
        prefs.minRating = scanner.nextDouble();

        System.out.print("Enter maximum runtime (in minutes): ");
        while (!scanner.hasNextInt()) {
            System.out.print("Please enter a valid integer for max runtime: ");
            scanner.next();
        }
        prefs.maxRuntime = scanner.nextInt();

        List<Movie> movies = loadMovies("movie_dataset.csv");

        List<Map.Entry<Movie, Double>> scoredMovies = new ArrayList<>();
        for (Movie movie : movies) {
            double score = calculateScore(movie, prefs);
            scoredMovies.add(new AbstractMap.SimpleEntry<>(movie, score));
        }

        scoredMovies.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        System.out.println("\nTop 10 movie recommendations:");
        for (int i = 0; i < Math.min(10, scoredMovies.size()); i++) {
            Movie m = scoredMovies.get(i).getKey();
            double score = scoredMovies.get(i).getValue();
            System.out.printf("%d) %s (Vote Avg: %.1f, Runtime: %d mins) - Score: %.2f%n",
                    (i + 1), m.title, m.voteAverage, m.runtime, score);
        }
        scanner.close();
    }
}
