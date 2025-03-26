import java.io.*;
import java.util.*;

public class FuzzyMovieRanking {

    static class Movie {
        String index, budget, genres, homepage, id, keywords, originalLanguage, originalTitle, overview;
        double popularity;
        String productionCompanies, productionCountries, releaseDate, revenue;
        int runtime;
        String spokenLanguages, status, tagline, title;
        double voteAverage;
        String voteCount, cast, crew, director;

        Movie(String index, String budget, String genres, String homepage, String id, String keywords,
              String originalLanguage, String originalTitle, String overview, double popularity,
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
        String preferredKeywords;
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
                String index = tokens[0],
                        budget = tokens[1],
                        genres = tokens[2],
                        homepage = tokens[3],
                        id = tokens[4],
                        keywords = tokens[5],
                        originalLanguage = tokens[6],
                        originalTitle = tokens[7],
                        overview = tokens[8];
                double popularity = 0.0;
                try {
                    popularity = Double.parseDouble(tokens[9]);
                } catch (NumberFormatException e) { }
                String productionCompanies = tokens[10],
                        productionCountries = tokens[11],
                        releaseDate = tokens[12],
                        revenue = tokens[13];
                int runtime = 0;
                try {
                    runtime = Integer.parseInt(tokens[14]);
                } catch (NumberFormatException e) { }
                String spokenLanguages = tokens[15],
                        status = tokens[16],
                        tagline = tokens[17],
                        title = tokens[18];
                double voteAverage = 0.0;
                try {
                    voteAverage = Double.parseDouble(tokens[19]);
                } catch (NumberFormatException e) { }
                String voteCount = tokens[20],
                        cast = tokens[21],
                        crew = tokens[22],
                        director = tokens[23];
                Movie movie = new Movie(index, budget, genres, homepage, id, keywords, originalLanguage,
                        originalTitle, overview, popularity, productionCompanies, productionCountries,
                        releaseDate, revenue, runtime, spokenLanguages, status, tagline, title,
                        voteAverage, voteCount, cast, crew, director);
                movies.add(movie);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + filename);
            e.printStackTrace();
        }
        return movies;
    }

    // New fuzzy scoring function.
    // We use:
    // - ratingScore: normalized difference between movie.voteAverage and prefs.minRating (using maxRating for scaling)
    // - runtimeScore: higher for movies with runtime much lower than prefs.maxRuntime.
    // - genreScore: fraction of user-specified genres found in movie.genres.
    // - keywordScore: fraction of user-specified keywords found in movie.keywords.
    // - popularityScore: normalized movie.popularity (using maxPopularity for scaling)
    // We combine these with weights.
    public static double calculateScore(Movie movie, UserPreferences prefs, double maxRating, double maxPopularity) {
        // Rating score
        double ratingScore = 0.0;
        if (movie.voteAverage >= prefs.minRating && maxRating > prefs.minRating) {
            ratingScore = (movie.voteAverage - prefs.minRating) / (maxRating - prefs.minRating);
            ratingScore = Math.min(ratingScore, 1.0);
        }
        // Runtime score
        double runtimeScore = (movie.runtime <= prefs.maxRuntime) ?
                ((prefs.maxRuntime - movie.runtime) / (double) prefs.maxRuntime) : 0.0;
        // Genre score
        Set<String> desiredGenres = new HashSet<>();
        for (String s : prefs.preferredGenre.split(",")) {
            if (!s.trim().isEmpty()) {
                desiredGenres.add(s.trim().toLowerCase());
            }
        }
        String[] movieGenres = movie.genres.split("\\s+");
        int genreMatchCount = 0;
        for (String token : movieGenres) {
            if (desiredGenres.contains(token.toLowerCase())) {
                genreMatchCount++;
            }
        }
        double genreScore = (desiredGenres.size() > 0) ? ((double) genreMatchCount / desiredGenres.size()) : 0.0;
        // Keyword score
        Set<String> desiredKeywords = new HashSet<>();
        for (String s : prefs.preferredKeywords.split(",")) {
            if (!s.trim().isEmpty()) {
                desiredKeywords.add(s.trim().toLowerCase());
            }
        }
        String[] movieKeywords = movie.keywords.split("\\s+");
        int keywordMatchCount = 0;
        for (String token : movieKeywords) {
            if (desiredKeywords.contains(token.toLowerCase())) {
                keywordMatchCount++;
            }
        }
        double keywordScore = (desiredKeywords.size() > 0) ? ((double) keywordMatchCount / desiredKeywords.size()) : 0.0;
        double popularityScore = (maxPopularity > 0) ? Math.min(movie.popularity / maxPopularity, 1.0) : 0.0;

//        double ratingWeight = 0.35;
//        double runtimeWeight = 0.15;
//        double genreWeight = 0.20;
//        double keywordWeight = 0.15;
//        double popularityWeight = 0.15;

        double ratingWeight = 0.3;
        double runtimeWeight = 0.2;
        double genreWeight = 0.5;
        double keywordWeight = 0.4;
        double popularityWeight = 0.3;
        return ratingWeight * ratingScore + runtimeWeight * runtimeScore + genreWeight * genreScore +
                keywordWeight * keywordScore + popularityWeight * popularityScore;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        UserPreferences prefs = new UserPreferences();

        System.out.print("Enter your preferred genre(s) (comma-separated): ");
        prefs.preferredGenre = scanner.nextLine().trim();

        System.out.print("Enter your preferred keyword(s) (comma-separated): ");
        prefs.preferredKeywords = scanner.nextLine().trim();

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

        double maxRating = 0.0;
        double maxPopularity = 0.0;
        for (Movie m : movies) {
            if (m.voteAverage > maxRating) {
                maxRating = m.voteAverage;
            }
            if (m.popularity > maxPopularity) {
                maxPopularity = m.popularity;
            }
        }

        List<Map.Entry<Movie, Double>> scoredMovies = new ArrayList<>();
        for (Movie movie : movies) {
            double score = calculateScore(movie, prefs, maxRating, maxPopularity);
            scoredMovies.add(new AbstractMap.SimpleEntry<>(movie, score));
        }
        scoredMovies.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        System.out.println("\nTop 10 movie recommendations:\n");
        for (int i = 0; i < Math.min(10, scoredMovies.size()); i++) {
            Movie m = scoredMovies.get(i).getKey();
            double score = scoredMovies.get(i).getValue();

            System.out.println("Movie #" + (i + 1));
            System.out.println("Original Title: " + m.originalTitle);
            System.out.println("Genres: " + m.genres);
            System.out.println("Keywords: " + m.keywords);
            System.out.println("Overview: " + m.overview);
            System.out.println("Homepage: " + m.homepage);
            System.out.println("Vote Average: " + m.voteAverage);
            System.out.println("Vote Count: " + m.voteCount);
            System.out.println("Popularity: " + m.popularity);
            System.out.println("Release Date: " + m.releaseDate);
            System.out.println("Runtime: " + m.runtime + " mins");
            System.out.println("Fuzzy Score: " + score);
            System.out.println("-----------------------------------------------------");
        }
        scanner.close();
    }
}
