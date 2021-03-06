package org.lenskit.mooc.cbf;

import org.lenskit.data.ratings.Rating;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Build a user profile from all positive ratings.
 */
public class WeightedUserProfileBuilder implements UserProfileBuilder {
    /**
     * The tag model, to get item tag vectors.
     */
    private final TFIDFModel model;

    @Inject
    public WeightedUserProfileBuilder(TFIDFModel m) {
        model = m;
    }

    @Override
    public Map<String, Double> makeUserProfile(@Nonnull List<Rating> ratings) {
        // Create a new vector over tags to accumulate the user profile
        Map<String, Double> profile = new HashMap<>();

        // TODO Normalize the user's ratings
        // TODO Build the user's weighted profile

        // getting the mean rating
        double countRating = 0.0;
        double sumRatings = 0.0;

        for (Rating r : ratings) {
            double currentRating = r.getValue();
            sumRatings += currentRating;
            countRating += 1.0;
        }

        double meanRating = sumRatings / countRating;

        for (Rating r : ratings) {
            long currentItem = r.getItemId();
            Map<String, Double> itemVector = model.getItemVector(currentItem);
            for (Map.Entry<String, Double> entry : itemVector.entrySet()) {
                String tagName = entry.getKey();
                double value = entry.getValue();
                double ratingValue = r.getValue();
                double normValue = value * (ratingValue - meanRating);
                double profTagValue = normValue;
                if (profile.containsKey(tagName)) {
                    double previousValue = profile.get(tagName);
                    profTagValue += previousValue;
                }
                profile.put(tagName, profTagValue);

            }
        }

        // The profile is accumulated, return it.
        return profile;
    }
}
