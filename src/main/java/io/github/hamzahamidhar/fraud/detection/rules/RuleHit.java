package io.github.hamzahamidhar.fraud.detection.rules;

/**
 * What a rule says when it spots something.
 *
 * @param ruleName short name of the rule, useful for logs and statistics
 * @param points   how much this adds to the risk score (out of 100)
 * @param reason   a plain-English sentence a fraud analyst could read
 */
public record RuleHit(String ruleName, int points, String reason) {
}
