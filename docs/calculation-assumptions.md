# Calculation assumptions and limits

These rules describe `main` at `83bd243` and are retained by the November source preview at `71b4733`. The published `v1.0.2-preview.1` APK predates the later unit-change and dilution-display work; see [release and verification status](verification.md).

Check results against the applicable approved SOP, analytical method, and laboratory QA system. Lab Calculator is a personal calculation aid, not an EPA-approved method or a validated laboratory information system. Automated tests do not establish suitability for a particular method.

Dilution uses the **final solution volume**, not the volume of solvent added. Results say “Transfer X µL/mL of stock and make up to Y mL final solution volume.” Stock and target must use the same concentration family and underlying basis. The app converts exactly within either **PPM/PPB** (1 PPM = 1,000 PPB on the same parts-per basis) or **mg/L/µg/L** (1 mg/L = 1,000 µg/L). It never equates PPM with mg/L or PPB with µg/L and does not infer density or which parts-per ratio basis your method uses. Existing PPM/PPB presets retain their original interpretation.

Changing a concentration unit converts populated numeric values exactly to preserve the quantity. MS/MSD converts source, spike, MS and MSD together; dilution converts the selected field. Blank fields stay blank. Invalid numeric text or an out-of-range conversion blocks the entire change, leaving values and units unchanged. Numeric values that fail a calculation rule (such as a zero spike) keep that value and its validation error after conversion. Changing concentration family clears all concentrations for re-entry while retaining dilution volume or MS/MSD dilution factor and their errors. If any affected concentration contains nonblank text, including invalid text, **Clear and change** requires confirmation; all-blank concentrations change family immediately. Cancel keeps the form unchanged. Changes clear results and steps while preserving unrelated validation errors. Selecting the current unit preserves state.

Molarity's volume selector and the standalone converter's starting selector and swap follow the same quantity-preserving policy. A converter destination change only selects an output unit; a category change requires confirming reset/re-entry when input is nonblank and changes immediately when empty. Preset loading replaces the saved preparation quantities with their saved units and clears results; it does not convert them from the previous form.

Terminating dilution quantities with up to 12 significant digits are displayed exactly. Other quantities, including repeating fractions, are marked `≈` and rounded half up to six significant digits. Scientific notation is used when plain notation exceeds 16 characters. Unit selection uses the exact stock volume before display rounding. Calculation Steps retain exact stock and final volumes, with reduced fractions for nonterminating results. Copying includes the complete instruction, units, and any approximation marks. This display policy does not describe pipette capability or measurement uncertainty; no intermediate arithmetic is rounded.

RPD uses `|A − B| / |(A + B) / 2| × 100`. A zero average is undefined. Signed results are accepted, but near-zero results and non-detects need your method's rules. The display uses two decimal places.

The MS/MSD calculator assumes **the spike was added after dilution**. Enter uncorrected source, MS, and MSD results on the same dilution basis, with the spike as its final added concentration. The dilution factor only reconstructs the original source concentration. RPD compares measured concentrations, not percent recoveries. This assumes the spike volume does not materially change the native sample concentration. When the MS/MSD average is zero, RPD is shown as undefined; source concentration, recoveries, and their calculation steps remain available. There are no automatic QC pass/fail limits.

Molarity / mass uses `m = M × V × FW`, with volume converted to liters. Use the formula weight for the actual reagent, including hydration. No purity correction is applied. Five significant figures are a display choice, not an assessment of the precision of your measurements.

Arithmetic uses decimal numbers rather than binary floating point. Input accepts a decimal point and scientific notation, with a 256-character limit and decimal scale from −1000 to 1000. Commas and non-detect qualifiers such as `<0.1` aren't interpreted as numbers. Exact arithmetic doesn't replace measurement uncertainty or your SOP's rounding rules.


## Additional input and workflow limits

- Dilution requires a positive stock concentration and final volume; target concentration may be zero but cannot exceed stock. A calculated zero-stock transfer is arithmetic, not a validated blank-preparation procedure.
- MS/MSD requires a positive added-spike concentration and factor. Factors below one are accepted by the implementation; confirm whether that interpretation fits the method. Signed results, negative recoveries, and recoveries above 100% are not automatically rejected.
- The converter supports µg/mg/g, µL/mL/L, and ng/L/µg/L/mg/L/g/L within their respective categories. It does not convert between mass, volume, and concentration categories.
- No pre-dilution spike model, significant spike-volume correction, recovery-based RPD mode, density conversion, purity correction, serial-dilution planner, instrument capability check, or automatic QC acceptance limits are provided.

The [historical audit](audit.md#calculation-verification) includes equations, dimensional checks, and worked examples. The [concentration-unit notes](concentration-units.md) explain compatible conversions and reset behavior; the [dilution notes](dilution-presets.md) give the full transfer-display policy.
