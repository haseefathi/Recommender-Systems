package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.data.dao.DataAccessObject;
import org.lenskit.data.ratings.Rating;
import org.lenskit.inject.Transient;
import org.lenskit.util.io.ObjectStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Iterator;

/**
 * Provider class that builds the mean rating item scorer, computing item means from the ratings in the DAO.
 */
public class ItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(ItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao The data access object (DAO), where the builder will get ratings.  The {@code @Transient} annotation
     *            on this parameter means that the DAO will be used to build the model, but the model will
     *            <strong>not</strong> retain a reference to the DAO.  This is standard procedure for LensKit models.
     */
    @Inject
    public ItemMeanModelProvider(@Transient DataAccessObject dao) {
        this.dao = dao;
    }

    /**
     * Construct an item mean model.
     *
     * <p>The {@link Provider#get()} method constructs whatever object the provider class is intended to build.</p>
     *
     * @return The item mean model with mean ratings for all items.
     */
    @Override
    public ItemMeanModel get() {
        // TODO Set up data structures for computing means
        Long2DoubleOpenHashMap movieRatingsSum = new Long2DoubleOpenHashMap(); // stores movieId => sumOfAllRatings for
        // that movie
        movieRatingsSum.defaultReturnValue(0.0);

        Long2DoubleOpenHashMap movieCounts = new Long2DoubleOpenHashMap(); // stores movieId => number of times movie
        // is rated
        movieCounts.defaultReturnValue(0.0);


        try (ObjectStream<Rating> ratings = dao.query(Rating.class).stream()) {
            for (Rating r : ratings) {
                // this loop will run once for each rating in the data set
                // TODO process this rating
                long movieId = r.getItemId();
                double currentRating = r.getValue();

                double newRatingsSum = movieRatingsSum.get(movieId) + currentRating;
                double newRatingCount = movieCounts.get(movieId) + 1;

                movieRatingsSum.put(movieId, newRatingsSum);
                movieCounts.put(movieId, newRatingCount);
            }
            // System.out.println(movieCounts);
            // System.out.println(movieRatingsSum);
        }


        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();
        Iterator iterator = movieRatingsSum.keySet().iterator();
        // TODO Finalize means to store them in the mean model
        while (iterator.hasNext()) {
            long movieId = (long) iterator.next();
            double sumOfAllRatings = movieRatingsSum.get(movieId);
            double ratingCount = movieCounts.get(movieId);
            double avg = sumOfAllRatings / ratingCount;

            means.put(movieId, avg);

        }

        System.out.println(means.toString());
        logger.info("computed mean ratings for {} items", means.size());
        return new ItemMeanModel(means);
    }
}
