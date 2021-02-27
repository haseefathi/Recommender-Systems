package org.lenskit.mooc.nonpers.mean;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.lenskit.baseline.MeanDamping;
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
 * Provider class that builds the mean rating item scorer, computing damped item means from the ratings in the DAO.
 */
public class DampedItemMeanModelProvider implements Provider<ItemMeanModel> {
    /**
     * A logger that you can use to emit debug messages.
     */
    private static final Logger logger = LoggerFactory.getLogger(DampedItemMeanModelProvider.class);

    /**
     * The data access object, to be used when computing the mean ratings.
     */
    private final DataAccessObject dao;
    /**
     * The damping factor.
     */
    private final double damping;

    /**
     * Constructor for the mean item score provider.
     *
     * <p>The {@code @Inject} annotation tells LensKit to use this constructor.
     *
     * @param dao     The data access object (DAO), where the builder will get ratings.  The {@code @Transient}
     *                annotation on this parameter means that the DAO will be used to build the model, but the model
     *                will <strong>not</strong> retain a reference to the DAO.  This is standard procedure for LensKit
     *                models.
     * @param damping The damping factor for Bayesian damping.  This is number of fake global-mean ratings to assume. It
     *                is provided as a parameter so that it can be reconfigured.  See the file {@code
     *                damped-mean.groovy} for how it is used.
     */
    @Inject
    public DampedItemMeanModelProvider(@Transient DataAccessObject dao,
                                       @MeanDamping double damping) {
        this.dao = dao;
        this.damping = damping;
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
        // TODO Compute damped means
        Long2DoubleOpenHashMap movieRatingsSum = new Long2DoubleOpenHashMap(); // stores movieId => sumOfAllRatings for
        // that movie
        movieRatingsSum.defaultReturnValue(0.0);

        Long2DoubleOpenHashMap movieCounts = new Long2DoubleOpenHashMap(); // stores movieId => number of times movie
        // is rated
        movieCounts.defaultReturnValue(0.0);

        double allRatingsTotal = 0.0;
        double allRatingsCount = 0.0;

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

                allRatingsTotal += currentRating;
                allRatingsCount += 1;
            }
            System.out.println(allRatingsTotal);
            System.out.println(allRatingsCount);
        }

        double mu = 0.0;
        if (allRatingsCount > 0)
            mu = allRatingsTotal / allRatingsCount;

        Long2DoubleOpenHashMap means = new Long2DoubleOpenHashMap();
        Iterator iterator = movieRatingsSum.keySet().iterator();
        // TODO Finalize means to store them in the mean model
        while (iterator.hasNext()) {
            long movieId = (long) iterator.next();
            double sumOfAllRatings = movieRatingsSum.get(movieId);
            double ratingCount = movieCounts.get(movieId);

            double numerator = (sumOfAllRatings + damping * mu);
            double denominator = ratingCount + damping;
            double dampedMean = numerator / denominator;

            means.put(movieId, dampedMean);

        }

        return new ItemMeanModel(means);
    }
}
