"""Kế hoạch một đề: chia 200 câu thành các "unit" sinh độc lập (một cuộc hội thoại, một bài đọc...).

Cấu trúc lấy từ toeic_spec.py (blueprint), chủ đề/độ khó được chọn xác định theo (seed, số đề, unit)
để mỗi đề có nội dung khác nhau nhưng chạy lại vẫn ra cùng kế hoạch.
"""
import random
from dataclasses import dataclass, field

from . import toeic_spec

TOPICS = [
    "hr_hiring", "hr_training", "payroll_benefits", "finance_budget", "accounting_invoice",
    "marketing_campaign", "sales_negotiation", "customer_complaint", "customer_service_return",
    "manufacturing_quality", "logistics_shipping", "inventory_warehouse", "travel_booking",
    "hotel_reservation", "restaurant_catering", "real_estate_lease", "it_software_update",
    "it_helpdesk", "office_supplies", "office_relocation", "event_conference", "event_exhibition",
    "health_safety", "building_maintenance", "banking_loan", "insurance_claim", "public_transport",
    "retail_promotion", "healthcare_appointment", "legal_contract", "research_development",
    "environment_recycling", "education_workshop", "construction_project", "utility_service",
]

PART1_SCENES = [
    "woman typing on laptop in office", "man carrying cardboard boxes in warehouse",
    "people waiting at bus stop", "chef cooking in restaurant kitchen", "empty meeting room table chairs",
    "cars parked along city street", "workers wearing helmets at construction site",
    "woman watering plants", "customers shopping in supermarket aisle", "man repairing bicycle",
    "people walking across pedestrian bridge", "waiter serving food at cafe table",
    "doctor talking with patient", "woman looking at shelves in library", "boats docked at harbor",
    "man using ladder to paint wall", "people shaking hands in office", "packages stacked on cart",
    "woman presenting to colleagues in conference room", "bicycles parked next to building",
    "man pushing luggage cart at airport", "technician inspecting machine in factory",
    "people sitting on bench in park", "cashier at checkout counter", "street vendor with fruit stand",
]

PART2_TYPES = ["when", "where", "who", "what", "why", "how", "how_much_many", "which", "yes_no",
               "tag_question", "statement", "choice_question", "suggestion_request"]

PART3_SITUATIONS = ["two_speakers", "two_speakers", "two_speakers", "three_speakers"]
PART4_TALKS = ["telephone_message", "announcement", "advertisement", "tour_guide", "radio_broadcast",
               "meeting_excerpt", "speech_introduction", "news_report", "recorded_instructions",
               "workshop_welcome"]
PART6_TEXTS = ["email", "memo", "notice", "advertisement", "article", "letter", "announcement"]
P7_SINGLE = ["email", "notice", "advertisement", "article", "form", "text_message_chain", "chat",
             "letter", "invoice", "memo", "schedule", "review", "web_page"]
P7_MULTI = [["email", "email"], ["advertisement", "email"], ["article", "email"], ["notice", "form"],
            ["email", "schedule"], ["web_page", "email"]]
P7_TRIPLE = [["email", "invoice", "email"], ["advertisement", "email", "form"],
             ["notice", "email", "schedule"], ["article", "email", "review"], ["memo", "email", "form"]]

# 30 câu Part 5/đề: 19 ngữ pháp + 11 từ vựng. Đề lẻ đổi 1 câu determiner -> conditional_inversion.
P5_TAGS = (["word_form"] * 4 + ["verb_tense"] * 3 + ["subject_verb_agreement"] * 2 + ["voice"] * 2
           + ["preposition_conjunction"] * 3 + ["pronoun", "relative_clause", "verbal_forms", "comparison",
                                                "determiner_quantifier"]
           + ["collocation"] * 5 + ["word_meaning"] * 4 + ["idiom_phrasal_verb"] * 2)
P5_LEVELS = [450] * 6 + [650] * 12 + [800] * 8 + [900] * 4


@dataclass
class Unit:
    id: str
    part: int
    kind: str                      # photo | qr | conversation | talk | p5 | p6 | p7
    group_ids: list                # group_id trong blueprint
    numbers: list                  # số câu (blueprint) mà unit này phụ trách
    params: dict = field(default_factory=dict)

    @property
    def size(self):
        return len(self.numbers)


def build_plan(test_index, seed=2026):
    rng = random.Random("%s-%s" % (seed, test_index))
    slots = toeic_spec.build_blueprint([1, 2, 3, 4, 5, 6, 7])
    groups = {}
    for s in slots:
        groups.setdefault(s["group_id"], []).append(s)

    topics = TOPICS[:]
    rng.shuffle(topics)
    topic_iter = iter(topics * 3)
    scenes = PART1_SCENES[:]
    rng.shuffle(scenes)

    units = []

    def group_unit(gid, kind, **params):
        sl = groups[gid]
        units.append(Unit("t%02d-%s" % (test_index, gid), sl[0]["part"], kind, [gid],
                          [x["number"] for x in sl], params))

    for i, gid in enumerate(sorted(g for g in groups if g.startswith("p1-"))):
        group_unit(gid, "photo", scene_query=scenes[i])

    # Part 2: 5 batch x 5 câu, mỗi batch có 5 dạng câu hỏi khác nhau
    p2 = sorted(g for g in groups if g.startswith("p2-"))
    types = PART2_TYPES[:]
    rng.shuffle(types)
    for b in range(5):
        chunk = p2[b * 5:(b + 1) * 5]
        qtypes = [types[(b * 5 + j) % len(types)] for j in range(5)]
        units.append(Unit("t%02d-p2-b%d" % (test_index, b + 1), 2, "qr", chunk,
                          [groups[g][0]["number"] for g in chunk],
                          {"types": qtypes, "topics": [next(topic_iter) for _ in range(5)]}))

    for gid in sorted(g for g in groups if g.startswith("p3-")):
        group_unit(gid, "conversation", topic=next(topic_iter), speakers=rng.choice(PART3_SITUATIONS),
                   level=rng.choice([450, 650, 650, 800, 900]))
    p4_kinds = PART4_TALKS[:]
    rng.shuffle(p4_kinds)
    for i, gid in enumerate(sorted(g for g in groups if g.startswith("p4-"))):
        group_unit(gid, "talk", topic=next(topic_iter), talk_type=p4_kinds[i],
                   level=rng.choice([450, 650, 650, 800, 900]))

    tags = ["conditional_inversion" if (t == "determiner_quantifier" and test_index % 2) else t
            for t in P5_TAGS]
    rng.shuffle(tags)
    levels = P5_LEVELS[:]
    rng.shuffle(levels)
    p5 = sorted(g for g in groups if g.startswith("p5-"))
    for b in range(6):
        chunk = p5[b * 5:(b + 1) * 5]
        units.append(Unit("t%02d-p5-b%d" % (test_index, b + 1), 5, "p5", chunk,
                          [groups[g][0]["number"] for g in chunk],
                          {"items": [{"skill_tag": tags[b * 5 + j], "level": levels[b * 5 + j],
                                      "topic": next(topic_iter)} for j in range(5)]}))

    p6_types = PART6_TEXTS[:]
    rng.shuffle(p6_types)
    for i, gid in enumerate(sorted(g for g in groups if g.startswith("p6-"))):
        group_unit(gid, "p6", topic=next(topic_iter), text_type=p6_types[i], level=[650, 800, 650, 900][i])

    multi = P7_MULTI[:]
    rng.shuffle(multi)
    triple = P7_TRIPLE[:]
    rng.shuffle(triple)
    singles = P7_SINGLE[:]
    rng.shuffle(singles)
    s_i = d_i = t_i = 0
    for gid in sorted(g for g in groups if g.startswith("p7-")):
        ptype = groups[gid][0]["passage_type"]
        if ptype == "single":
            docs = [singles[s_i % len(singles)]]
            s_i += 1
        elif ptype == "double":
            docs = multi[d_i]
            d_i += 1
        else:
            docs = triple[t_i]
            t_i += 1
        group_unit(gid, "p7", topic=next(topic_iter), documents=docs, passage_type=ptype,
                   level=rng.choice([650, 800, 800, 900]))
    return units


def total_questions(units):
    return sum(u.size for u in units)
