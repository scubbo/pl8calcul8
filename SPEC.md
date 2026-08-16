We're going to collaborate on building a system. The system will have two parts:
* A client - an Android App - which is used as a weightlifting weight tracker. The operation of the app is described below.
* A server, which serves only to provide optional backup for the client app's data.

I am entirely unexperienced with Android App development, so you're going to have to walk me through it from first principles.

I don't have any intent to publish the app on the App Store - it's entirely for personal use.

# App functionality

## Terminology

* RPE is as defined below
* A lift is a particular way of using weights to stress muscles. Examples of lifts are bench press, deadlift, overhead press, and bicep curl. Weightlifters track their completed weights for particular lifts.
* A rep is a single execution of a lift.
* A set is a sequence of reps in quick succession - typically 4-10, but sometimes as high as 20.
* An exercise is completing multiple sets (typically at the same weight) with a break in between.
* An "exercise definition" is an instruction given by a coach to complete an exercise at a given RPE - e.g. "Bench Press 4@7*3" means "complete 3 sets of 4 reps of Bench Press at RPE 7"
* A workout is a collection of exercises - usually 3-4.

## RPE

To understand the app's logic, you must first understand RPE - Rate of Perceived Exertion. This is a measurement of the difficulty of a given weightlifting set, where a "set" is a certain number of reps (executions) of the lift at a certain weight. RPE is a numerical measure in the range 1-10, calculated as "10 minus the number of additional reps you feel you could have done". For instance, if you complete a set and feel that you could have completed 3 more reps, that was RPE 7. If the set fully exhausted you and you could not do another rep, it was RPE 10. RPE is often colloquially expressed as half-integers - "RPE 7.5" - to indicate that the perceived exhaustion lies somewhere between those two values.

RPE is also used to set the target weight for a given exercise when assigned by a coach - e.g. "4@7" means "a set of 4 reps at RPE 7". To calculate this target weight, a series of ratios can be used to map between any <rep count, RPE> pair and the implied 1RM (1 Rep Max). By combining two ratios (one dividing, one multiplying), a lifter can infer "my X@Y weight was W_1, so my A@B weight should be about W_2". Thus, based on their most-recent rep-count-and-RPE, the lifter can calculate the weight for a new assigned rep-count-and-RPE.

I have a table of all the given ratios, which will form a core part of the app's calculation logic - I can provide it when needed.

## App functionality

* Tracking weights for lifts over time - actual weights, and also calculated 1RMs for graphing
* Given a collection of exercise definitions, advise on the weights to use, with the following logic:
  * For each lift, look up the most-recent completed exercise, and the recorded weight and RPE
  * Use ratios to calculate the appropriate weight.
  * To encourage development, the app should increment the weight slightly each time (by an amount configurable per lift, but defaulting to 5lb), rounding down to the nearest 5lb. For instance, if the ratio calculation from the previous lift comes out to 202.5lb and the increment amount for that lift is 10lb, the advised weight should be (`round-down(202.5 + 10) = round-down(212.5) =`) 210.

## Server

The server will be self-hosted and should just expose minimal endpoints to:
* Upload data from the client app to back it up
* Seed a newly-installed app from back up
* (Stretch goal - not P0) provide a web interface to view history

# Development plan

As stated above, I have no familiarity with Android app development. We should prioritze setting up a local emulation environment so that I can view and interact with the app locally for fast development. I have no preference on language or stack.

Before getting started, interview me extensively on the app's UX and features so that you know what we're building.

Make regular git commits as you go.
