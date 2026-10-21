# Learning this project

This is a study guide for working through the project file by file, until you can explain every part of it in an interview without notes.

Go in the order below. Each step builds on the one before. For every file, read it top to bottom, then close it and try to explain it out loud in two or three sentences. If you can't, read it again.

At the end of each step there are the questions an interviewer is likely to ask, with short answers. Cover the answers and try them yourself first.

**Total time:** about 12–15 hours, spread over a week or two.

---

## Step 1: The big picture (1 hour)

Read `README.md` from start to finish, then run the app and play with the dashboard. Submit an ordinary payment, then a suspicious one. Look at `/api/model/evaluation` in your browser.

Then try to explain the whole project in 60 seconds. This is your answer to "tell me about a project you've built", so practise it until it's smooth. Something like:

> "It's a Spring Boot service that scores card payments for fraud. When a payment comes in, it loads the customer's recent history from the database, works out what's normal for them, and runs two things: five rules, like 'three times their usual spend' or 'first payment from a new country', and a logistic regression model I wrote from scratch. It blends the two into a risk score and decides to approve, review or block, with reasons in plain English. I trained and tested it on synthetic data, split by customer so the test was honest. Combined, it caught 93% of fraud while flagging about 2.6% of genuine payments, which beat either the rules or the model on their own."

**Likely questions**

- *Why did you build this?* To practise the stack banks use (Java, Spring Boot, SQL, REST, tests) on a problem where AI and good engineering both matter, and where decisions have to be explainable.
- *Why synthetic data?* Real transaction data is private. Public fraud datasets mostly have anonymised columns, which makes readable rules impossible. Generating my own means I know exactly what's in it, though the catch is that it's easier than the real world.

---

## Step 2: The plain data types (1 hour)

Read these short files first, because everything else uses them:

1. `domain/MerchantCategory.java`: an enum with extra data attached (a label and a high-risk flag).
2. `domain/Decision.java`: APPROVE, REVIEW, BLOCK.
3. `detection/TransactionDetails.java`: a Java **record**. Make sure you understand records: they're immutable data classes, and Java writes the constructor, getters, `equals`, `hashCode` and `toString` for you.
4. `detection/Formatting.java`: small helpers.

**Likely questions**

- *Why `BigDecimal` for money and not `double`?* Doubles are stored in binary and can't represent many decimals exactly (0.1 + 0.2 isn't exactly 0.3). With money, those tiny errors add up and aren't acceptable.
- *What's a record?* A compact way to declare an immutable class that just holds data.
- *Why REVIEW and not just fraud / not fraud?* Wrongly blocking a genuine customer has a real cost too. A middle option lets a person or the customer confirm before acting.

---

## Step 3: The customer profile (1.5 hours)

Read `detection/CustomerProfile.java` slowly. This is the most important idea in the project: a payment is only suspicious compared with the customer's own normal behaviour.

Things to understand:
- `fromHistory` only uses payments **before** `asOf`, and only from the last 90 days.
- Why it uses the **median**, not the average.
- What "established history" means (at least 5 payments), and why the rules behave differently for new customers.

Then read `CustomerProfileTest.java` and see how each test checks one behaviour.

**Likely questions**

- *Why median instead of mean?* One big purchase pulls the mean up a lot, which would make later big payments look normal. The median barely moves.
- *Why only look at the past?* That's what the bank knows at the moment of payment. Using later data would be "data leakage" and make results look better than they really are.

---

## Step 4: The rules (2 hours)

Read `rules/FraudRule.java` first (the interface), then `RuleHit.java`, then each rule. They're all short and follow the same pattern.

Things to understand:
- What an **interface** is, and why every rule implements the same one. (`FraudDetector` can loop over a list of rules without knowing what each one does. Adding a rule means one new class and no other changes. That's the "open/closed principle".)
- What `Optional` is, and why `check` returns `Optional<RuleHit>` instead of `null`.
- For each rule: what it catches, and the genuine behaviour that could trigger it by mistake.

Then read `FraudRulesTest.java`. Notice each rule has tests for when it should fire **and** when it should stay quiet.

**Likely questions**

- *How did you choose the thresholds and points?* By reasoning about how strong each signal is: amount and bursts are strong, new country is medium because people travel, night time is weak. The model then gives a data-driven second opinion. In a real bank, thresholds would be tuned on historical data.
- *What's a weakness of rules?* They only catch what you thought of, and thresholds are cliffs: 7.9x and 8.1x normal get very different scores.
- *Why `Optional`?* It makes "there might be no result" part of the method's type, so the caller can't forget to handle it, unlike `null`.

---

## Step 5: The model (3 hours)

This is the part most likely to get deep questions for AI and data roles. Take your time.

1. `model/FeatureExtractor.java`: how a payment becomes eight numbers. For each feature, be able to say what it means and why it might predict fraud.
2. `model/LogisticRegressionModel.java`: read the class comment first, then `train`, then `predictProbability`.

Understand these ideas well enough to draw them on a whiteboard:
- **Sigmoid:** squashes any number into 0–1, so the output can be read as a probability.
- **Weights:** one per feature. Positive pushes towards fraud, negative pushes away. Look at the learned weights in `/api/model/evaluation`, and be ready to say why "many payments in a short time" gets a big positive weight and "little or no history" gets a negative one. (In this synthetic data, fraud only happens to customers who already have history, so a brand new customer is actually less likely to be fraud. That's an artefact of how I generated the data, and a good example of a model learning something that wouldn't hold in real life.)
- **Gradient descent:** start with zero weights, measure how wrong the predictions are, nudge each weight a little in the direction that reduces the error, and repeat 800 times.
- **Standardisation:** rescaling each feature to mean 0, standard deviation 1, so features with big numbers don't drown out 0/1 features.
- **Class weights:** fraud is about 2% of the data, so each fraud example counts for more during training. Otherwise the model could score well by always saying "not fraud".
- **L2 penalty:** gently pulls weights towards zero, so the model doesn't overreact to a few odd examples (it reduces overfitting).

Then read `LogisticRegressionModelTest.java`. The first test is a neat trick worth mentioning: a toy problem where I know the right answer, to check the model can find it.

**Likely questions**

- *Why logistic regression and not something more powerful?* It's interpretable, so I can say exactly why a payment was flagged, which banks need. It's also fast, and with only eight features a fancier model wouldn't have much to gain. Next I'd compare it with gradient-boosted trees.
- *Why write it yourself instead of using a library?* To understand it properly, and because the whole thing is under 200 lines. In production I'd use a tested library.
- *What does a weight of 0.85 mean?* Each standard deviation increase in that feature multiplies the odds of fraud by about e^0.85 ≈ 2.3, holding the others fixed.
- *Isn't the probability inflated by the class weights?* Yes. Class weighting makes the model predict fraud more readily, so the numbers aren't true real-world probabilities. That's fine for ranking risk, but they'd need recalibrating before being reported as real probabilities.

---

## Step 6: Putting rules and model together (1 hour)

Read `detection/FraudDetector.java` and `FraudAssessment.java`, then `FraudDetectorTest.java`.

Understand how the final score is calculated (half rules, half model), the thresholds for REVIEW and BLOCK, and how the reasons are put together.

**Likely questions**

- *Why 50/50?* It's a simple, explainable starting point, and the evaluation shows it beats either one alone. Ideally the weights and thresholds would be tuned on a separate validation set.
- *How would you choose the thresholds in a real bank?* Based on cost. A missed fraud costs the fraud amount; a false alarm costs customer annoyance and analyst time. You'd set the threshold where the total cost is lowest, and you'd also be limited by how many reviews the analysts can handle each day.

---

## Step 7: Synthetic data and evaluation (2 hours)

1. `simulation/SyntheticDataGenerator.java`: how customers, normal spending, "suspicious-looking but genuine" behaviour, and three fraud patterns are created. Notice the seeded `Random`.
2. `evaluation/ExampleBuilder.java`: replays payments in time order.
3. `evaluation/Metrics.java`: precision, recall, F1, false positive rate. Make sure you can work these out by hand from the four counts. (`EvaluationTest` has a worked example.)
4. `evaluation/TrainingPipeline.java`: the full process, especially the split by customer.

**Likely questions**

- *Why not just report accuracy?* With 2% fraud, flagging nothing scores 98%. Precision and recall show what's actually happening.
- *What's the difference between precision and recall?* Precision: when I flag something, how often am I right? Recall: of all the fraud, how much did I find? Raising one usually lowers the other.
- *Why split by customer?* So that no customer appears in both training and testing. That tests the model on genuinely new people, which is what matters in real use.
- *Why a seed?* So the same seed gives identical data and results every time, which makes the results repeatable and the tests reliable.
- *How do you know it's not just memorising your own patterns?* Partly, it is, and that's the honest limitation of synthetic data. I reduced it by adding genuine behaviour that looks suspicious, and a subtle fraud pattern that looks normal. Real data is the true test.

---

## Step 8: The Spring Boot layers (3 hours)

Now the part that turns the logic into a working service. Read in this order:

1. `FraudDetectionApplication.java`: the starting point.
2. `domain/CardTransaction.java`: a JPA **entity**. Each annotation maps the class onto a database table.
3. `repository/TransactionRepository.java`: Spring Data writes the SQL from the method names.
4. `service/TransactionService.java`: the business logic for a new payment.
5. `api/TransactionRequest.java`, `TransactionResponse.java`, `ApiError.java`.
6. `api/TransactionController.java`: maps URLs to methods.
7. `api/ApiExceptionHandler.java`: turns errors into clean JSON.
8. `config/DetectionConfig.java` and `DemoDataLoader.java`.
9. `src/main/resources/application.properties`.

Then read `TransactionApiTest.java`, which tests the whole thing through HTTP.

Concepts to be confident about:
- **Layered architecture:** controller (HTTP) → service (business logic) → repository (database). Each has one job.
- **Dependency injection:** classes ask for what they need in their constructor, and Spring provides it. This makes classes easy to test, because you can pass in different versions.
- **Beans:** the objects Spring creates and manages (`@Service`, `@RestController`, `@Bean` methods, and so on).
- **JPA / Hibernate:** maps Java objects to database tables, so you rarely write SQL.
- **`@Transactional`:** everything in the method either all happens or none of it does.
- **REST basics:** GET reads, POST creates, status codes 200, 201, 400 and 404.
- **Validation:** `@Valid` plus the annotations on `TransactionRequest`.
- **DTOs:** why the API returns `TransactionResponse` and not the entity itself.

**Likely questions**

- *Why separate controller, service and repository?* Each layer has one responsibility, so it's easier to test and change. For example, I could swap H2 for PostgreSQL without touching the controller.
- *What does `@Transactional` do?* It wraps the method in a database transaction, so if something fails halfway, the changes are rolled back.
- *Why is there an index on customer_id and occurred_at?* Every new payment looks up that customer's recent payments. Without an index, the database would scan the whole table each time.
- *Why not return the entity from the controller?* It would tie the API to the database structure, and could expose fields by accident.
- *What's the difference between a unit test and an integration test here?* The rule tests check one class on its own, in milliseconds. `TransactionApiTest` starts the whole app and sends real HTTP requests through every layer.
- *How would this scale to millions of payments?* Use a real database like PostgreSQL, keep each customer's profile cached instead of rebuilding it from history each time, and process payments from a message queue such as Kafka.

---

## Step 9: Build and tooling (30 minutes)

Read `pom.xml`, `.github/workflows/build.yml` and `.gitignore`.

- *What does Maven do?* It downloads the libraries listed in `pom.xml`, compiles the code, runs the tests and packages the app.
- *What does the GitHub Actions file do?* On every push, it builds the project and runs all the tests on a clean machine. That's continuous integration (CI).

---

## Before an interview: final checklist

You're ready when you can do all of these without looking:

- [ ] Explain the project in 60 seconds
- [ ] Draw the flow from "payment arrives" to "decision saved"
- [ ] Name all five rules and what each one catches
- [ ] Explain sigmoid, weights, gradient descent and class weights
- [ ] Define precision, recall and false positive rate, and work them out from four counts
- [ ] Explain why the split is by customer
- [ ] Quote the results: about 93% recall, 2.6% false alarms, and why combined beats either alone
- [ ] Explain controller → service → repository, and dependency injection
- [ ] Name three limitations and what you'd build next
- [ ] Make one small change yourself, such as adding a new rule with tests, so you've actually written code in it

That last one matters most. Adding a rule (say, "payment to a merchant category this customer hasn't used in 60 days") means writing a class, adding it to `defaultRules()`, and writing two tests. Doing it yourself is the best way to prove to yourself, and to an interviewer, that you understand how it all fits together.
