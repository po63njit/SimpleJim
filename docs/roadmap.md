# SimpleJim roadmap

Keep the default flow short: pick a lift, tap in a set, repeat when useful, and save once. New capabilities should not force extra forms onto this flow.

| Slice | Scope | Not included in the current app |
| --- | --- | --- |
| 1. Cardio inputs and history | First add treadmill, StairMaster, and outdoor run/walk as clearly labeled manual activity types. Use duration plus appropriate fields: distance and incline where relevant, or floors for StairMaster. Validate units and show what was entered before deriving pace or speed. | No cardio screen, tracking permissions, sensor imports, metabolic estimates, or comparisons across incompatible units. |
| 2. Personal records and strength charts | Keep concise PR and estimated 1RM progress visible in the relevant exercise view, with full charts accessible without leaving the logging flow. Separate heaviest set, rep PRs, and estimated 1RM; name the estimate formula and say when the estimate is unreliable. | [Read-only exercise history](strength-history.md) includes a compact chart of observed load and provisional estimates for up to 12 sessions; logged singles and the full session trend are available in text. No rep-specific PR system, interactive chart controls, or always-visible indicators yet. |
| 3. Bodyweight tracking and strength ratios | Let the user deliberately log bodyweight. Define the direction of each ratio explicitly (for example, lift-to-bodyweight = lift weight ÷ eligible dated bodyweight, both in the same units). Leave unmatched values unavailable rather than silently guessing. | No bodyweight records or bodyweight ratios yet. |
| 4. Benchmarks and motivating progress | Introduce personal milestones before population comparisons. Use documented units, comparability rules, and data provenance for any outside benchmark, and keep the reference clearly separate from training advice. | No benchmark ratings or recommendations yet. |

Prefer raw observations as the source of truth. Derived values should be recomputable, tested at boundaries, and labeled with defensible units and assumptions. Ship one slice at a time and update the docs/tests with each behavior change. Cloud accounts, general sync, and store distribution are outside this staged roadmap.
