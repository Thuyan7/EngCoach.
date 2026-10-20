// MOCK DATA — SAMPLE QUESTIONS ONLY. Not real TOEIC content.
//
// The backend has no endpoint that returns question text yet (see
// SLICE-S7B-NOTES.md). Until it does, the test screen shows these so the
// screen can be built and exercised. The ids are deliberately huge so they
// can never be mistaken for a real Question row.
//
// Shape = the shape the real API is expected to provide, so replacing this
// is a one-file change (api/questions.js):
//   { id: number, prompt: string, options: [{ key: "A", text: string }, ...] }
// `chosenOption` sent to the server is the option `key`.
export function mockQuestionsFor(part) {
  const opts = (a, b, c, d) => [
    { key: "A", text: a },
    { key: "B", text: b },
    { key: "C", text: c },
    { key: "D", text: d },
  ];
  return [
    {
      id: 900001,
      prompt: `[Sample · ${part}] The manager asked everyone ____ the report by Friday.`,
      options: opts("submit", "to submit", "submitting", "submitted"),
    },
    {
      id: 900002,
      prompt: `[Sample · ${part}] Our new office is located ____ the train station.`,
      options: opts("near", "nearly", "nearness", "nearer to"),
    },
    {
      id: 900003,
      prompt: `[Sample · ${part}] Please ____ your badge at the front desk.`,
      options: opts("leaves", "leaving", "leave", "left"),
    },
    {
      id: 900004,
      prompt: `[Sample · ${part}] The meeting was postponed ____ the director was ill.`,
      options: opts("because", "despite", "so that", "although"),
    },
    {
      id: 900005,
      prompt: `[Sample · ${part}] ____ employees must attend the safety briefing.`,
      options: opts("Every", "All", "Each", "Another"),
    },
  ];
}
