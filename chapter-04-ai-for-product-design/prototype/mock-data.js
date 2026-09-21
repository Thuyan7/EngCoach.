// mock-data.js
// Static data used by the prototype. Not a real API, no persistence.
// Kept separate from markup/logic so app.js does not hardcode content inline.

var MOCK_USER = {
  firstName: "An",
  fullName: "An Nguyen",
  initial: "A"
};

var MOCK_DASHBOARD_STATS = {
  averageScore: 720,
  attemptCount: 14,
  lastAttempt: "2 days ago",
  bandPositionPercent: 68 // marker position on the qualitative band, placeholder until OQ-004 is resolved
};

var MOCK_QUESTION = {
  number: 42,
  total: 200,
  prompt: 'Which word best completes the sentence: "The manager asked the team to ___ the report by Friday."',
  options: [
    { id: "A", text: "complete" },
    { id: "B", text: "completion" },
    { id: "C", text: "completing" }
  ]
};

var MOCK_RESULT = {
  score: 165,
  total: 200,
  testLabel: "Reading & Listening mock test",
  explanations: [
    {
      questionNumber: 42,
      chosenOption: "B",
      status: "incorrect",
      correctOption: "A",
      text: 'Correct answer: A, complete. The sentence needs a base verb after "asked the team to," not a noun or gerund form.'
    },
    {
      questionNumber: 57,
      chosenOption: "C",
      status: "unavailable",
      text: "Explanation temporarily unavailable."
    }
  ]
};

var MOCK_PROGRESS_HISTORY = [
  { date: "Sep 12", type: "Mock test", score: 720, barHeightPercent: 82 },
  { date: "Sep 5", type: "Part 5 practice", score: 660, barHeightPercent: 75 },
  { date: "Sep 1", type: "Mock test", score: 610, barHeightPercent: 60 }
];

var MOCK_TIMER = {
  startMinutes: 21,
  warningThresholdMinutes: 5,
  dangerThresholdMinutes: 1
};

var MOCK_PARTS = [
  { name: "Part 5", meta: "Incomplete sentences", accuracy: 78 },
  { name: "Part 6", meta: "Text completion", accuracy: 64 },
  { name: "Part 7", meta: "Reading comprehension", accuracy: 71 },
  { name: "Part 2", meta: "Question and response", accuracy: 85 },
  { name: "Part 3", meta: "Conversations", accuracy: 59 },
  { name: "Part 4", meta: "Short talks", accuracy: 66 }
];

var MOCK_PART_BREAKDOWN = [
  { name: "Part 2 - Question and response", accuracy: 85 },
  { name: "Part 5 - Incomplete sentences", accuracy: 78 },
  { name: "Part 7 - Reading comprehension", accuracy: 71 },
  { name: "Part 4 - Short talks", accuracy: 66 },
  { name: "Part 3 - Conversations", accuracy: 59 }
];

// Answer state for the question navigator. Shows the first 25 of 200 questions
// so the navigator fits on screen in this prototype.
var MOCK_QUESTION_STATES = [
  "answered","answered","answered","answered","answered",
  "answered","answered","answered","answered","answered",
  "answered","answered","answered","answered","answered",
  "answered","answered","answered","answered","answered",
  "answered","current","","","" 
];
