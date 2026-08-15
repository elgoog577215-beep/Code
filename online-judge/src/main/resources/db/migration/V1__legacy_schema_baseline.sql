create table ai_diagnosis_runs (
        official_version boolean not null,
        result_saved boolean not null,
        version_number integer not null,
        completed_at timestamp(6),
        created_at timestamp(6) not null,
        id bigserial not null,
        started_at timestamp(6),
        submission_id bigint not null,
        updated_at timestamp(6) not null,
        status varchar(32) not null,
        generation_key varchar(64) not null,
        current_stage varchar(96),
        failure_reason TEXT,
        primary key (id),
        constraint uk_ai_diagnosis_run_generation unique (generation_key),
        constraint uk_ai_diagnosis_run_version unique (submission_id, version_number)
    );

    create table ai_diagnosis_stage_runs (
        attempt_count integer not null,
        completed_at timestamp(6),
        created_at timestamp(6) not null,
        id bigserial not null,
        latency_ms bigint,
        run_id bigint not null,
        started_at timestamp(6),
        updated_at timestamp(6) not null,
        status varchar(32) not null,
        stage_type varchar(64) not null,
        input_fingerprint varchar(96),
        issue_id varchar(120),
        prompt_version varchar(120),
        provider varchar(120),
        stage_key varchar(160) not null,
        model varchar(180),
        failure_reason TEXT,
        output_json TEXT,
        primary key (id),
        constraint uk_ai_diagnosis_stage_key unique (run_id, stage_key)
    );

    create table ai_standard_improvement_points (
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        category varchar(80) not null,
        library_version varchar(80) not null,
        name varchar(120) not null,
        code varchar(160) not null,
        primary_knowledge_node_code varchar(160),
        skill_unit_code varchar(160),
        applicable_languages varchar(800),
        improvement_goal varchar(1200),
        practice_strategy varchar(1200),
        student_benefit varchar(1200),
        teacher_explanation varchar(1200),
        description varchar(1600),
        related_mistake_codes varchar(1600),
        knowledge_node_codes varchar(2400),
        primary key (id),
        constraint uk_ai_standard_improvement_point_code unique (code)
    );

    create table ai_standard_library_growth_candidates (
        confidence float(53),
        occurrence_count integer,
        created_at timestamp(6) not null,
        id bigserial not null,
        last_observed_at timestamp(6),
        source_problem_id bigint,
        source_submission_id bigint,
        updated_at timestamp(6) not null,
        layer varchar(40) not null check (layer in ('SKILL_UNIT','MISTAKE_POINT','BASIC_CAUSE','IMPROVEMENT_POINT')),
        status varchar(40) not null check (status in ('PROPOSED','NEEDS_REVIEW','BLOCKED','MERGED_SIMILAR','TEACHER_APPROVED','REJECTED','MERGED','IGNORED')),
        evidence_status varchar(80),
        parent_knowledge_node_code varchar(120),
        suggested_code varchar(120) not null,
        suggested_name varchar(160) not null,
        suggested_path varchar(800),
        evidence_refs varchar(1200),
        similar_existing_items varchar(1200),
        change_reason varchar(1600),
        observed_submission_ids varchar(1600),
        precheck_message varchar(1600),
        teacher_note varchar(1600),
        before_snapshot TEXT,
        diff_summary TEXT,
        rollback_info TEXT,
        primary key (id)
    );

    create table ai_standard_library_items (
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        layer varchar(40) not null check (layer in ('SKILL_UNIT','MISTAKE_POINT','BASIC_CAUSE','IMPROVEMENT_POINT')),
        severity varchar(40),
        category varchar(80) not null,
        library_version varchar(80) not null,
        mistake_type varchar(80),
        code varchar(100) not null,
        ability_point varchar(120),
        name varchar(120) not null,
        teaching_action varchar(120),
        primary_knowledge_node_code varchar(160),
        skill_unit_code varchar(160),
        applicable_languages varchar(800),
        hintl1 varchar(800),
        hintl2 varchar(800),
        hintl3 varchar(800),
        student_benefit varchar(800),
        when_to_use varchar(800),
        judge_signals varchar(1200),
        student_explanation varchar(1200),
        teacher_explanation varchar(1200),
        common_misconception varchar(1600),
        description varchar(1600),
        related_items varchar(1600),
        required_evidence varchar(1600),
        common_code_patterns varchar(2400),
        evidence_signals varchar(2400),
        knowledge_node_codes varchar(2400),
        prerequisite_knowledge_codes varchar(2400),
        related_knowledge_node_codes varchar(2400),
        primary key (id),
        constraint uk_ai_standard_library_layer_code unique (layer, code)
    );

    create table ai_standard_library_legacy_mappings (
        confidence float(53) not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        legacy_layer varchar(40) not null check (legacy_layer in ('SKILL_UNIT','MISTAKE_POINT','BASIC_CAUSE','IMPROVEMENT_POINT')),
        migration_status varchar(40) not null,
        target_type varchar(40) not null check (target_type in ('KNOWLEDGE_NODE','SKILL_UNIT','MISTAKE_POINT','IMPROVEMENT_POINT')),
        source_version varchar(80),
        legacy_code varchar(160) not null,
        target_code varchar(160) not null,
        primary key (id),
        constraint uk_ai_standard_library_legacy_mapping unique (legacy_layer, legacy_code)
    );

    create table ai_standard_library_relations (
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        relation_type varchar(40) not null check (relation_type in ('PREREQUISITE','RELATED','CONFUSABLE','TRANSFER','EXTENDS')),
        source_type varchar(40) not null check (source_type in ('KNOWLEDGE_NODE','SKILL_UNIT','MISTAKE_POINT','IMPROVEMENT_POINT')),
        target_type varchar(40) not null check (target_type in ('KNOWLEDGE_NODE','SKILL_UNIT','MISTAKE_POINT','IMPROVEMENT_POINT')),
        source_code varchar(160) not null,
        target_code varchar(160) not null,
        description varchar(800),
        primary key (id),
        constraint uk_ai_standard_library_relation unique (source_type, source_code, relation_type, target_type, target_code)
    );

    create table ai_standard_mistake_points (
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        severity varchar(40),
        category varchar(80) not null,
        library_version varchar(80) not null,
        mistake_type varchar(80),
        name varchar(120) not null,
        code varchar(160) not null,
        primary_knowledge_node_code varchar(160) not null,
        skill_unit_code varchar(160) not null,
        applicable_languages varchar(800),
        repair_strategy varchar(1200),
        symptom varchar(1200),
        description varchar(1600),
        misconception varchar(1600),
        knowledge_node_codes varchar(2400),
        prerequisite_knowledge_codes varchar(2400),
        primary key (id),
        constraint uk_ai_standard_mistake_point_code unique (code)
    );

    create table ai_standard_skill_units (
        enabled boolean not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        mastery_level varchar(40),
        category varchar(80) not null,
        library_version varchar(80) not null,
        name varchar(120) not null,
        code varchar(160) not null,
        primary_knowledge_node_code varchar(160) not null,
        applicable_languages varchar(800),
        learning_goal varchar(1200),
        description varchar(1600),
        knowledge_node_codes varchar(2400),
        prerequisite_knowledge_codes varchar(2400),
        primary key (id),
        constraint uk_ai_standard_skill_unit_code unique (code)
    );

    create table ai_usage_events (
        attempt_no integer not null,
        charged boolean not null,
        input_tokens integer,
        output_tokens integer,
        quota_units integer not null,
        success boolean not null,
        assignment_id bigint,
        created_at timestamp(6) with time zone not null,
        id bigserial not null,
        student_profile_id bigint,
        submission_id bigint,
        teacher_id uuid not null,
        usage_purpose varchar(60) not null,
        provider varchar(80),
        idempotency_key varchar(160) not null,
        model varchar(160),
        failure_reason varchar(500),
        primary key (id)
    );

    create table assignment_invites (
        enabled boolean not null,
        assignment_id bigint not null,
        created_at timestamp(6) not null,
        expires_at timestamp(6),
        id bigserial not null,
        code varchar(255) not null unique,
        primary key (id)
    );

    create table assignment_recipients (
        assignment_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        student_profile_id bigint not null,
        primary key (id),
        constraint uk_assignment_recipient unique (assignment_id, student_profile_id)
    );

    create table assignment_tasks (
        order_index integer not null,
        required boolean not null,
        assignment_id bigint not null,
        id bigserial not null,
        problem_id bigint not null,
        primary key (id)
    );

    create table assignments (
        class_group_id bigint,
        created_at timestamp(6) not null,
        ends_at timestamp(6),
        id bigserial not null,
        starts_at timestamp(6),
        owner_teacher_id uuid not null,
        description TEXT,
        hint_policy varchar(255) not null check (hint_policy in ('L1','L2','L3','L4')),
        status varchar(255) not null check (status in ('DRAFT','ACTIVE','CLOSED')),
        target_mode varchar(255) not null check (target_mode in ('CLASS','STUDENTS')),
        title varchar(255) not null,
        primary key (id)
    );

    create table class_groups (
        created_at timestamp(6) not null,
        id bigserial not null,
        owner_teacher_id uuid not null,
        join_code_hash varchar(100),
        grade varchar(255),
        name varchar(255) not null,
        teacher_name varchar(255),
        primary key (id)
    );

    create table class_review_feedback (
        assignment_id bigint not null,
        created_at timestamp(6) not null,
        example_problem_id bigint,
        id bigserial not null,
        action_type varchar(255) not null,
        created_by varchar(255),
        evidence_tags TEXT,
        suggestion_key varchar(255) not null,
        target_ability varchar(255),
        teacher_note TEXT,
        primary key (id)
    );

    create table coach_prompts (
        turn_index integer,
        answered_at timestamp(6),
        assignment_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        parent_prompt_id bigint,
        student_profile_id bigint,
        submission_id bigint not null,
        coach_feedback TEXT,
        context_summary TEXT,
        evidence_refs TEXT,
        hint_policy varchar(255) not null,
        model_answer_leak_risk varchar(255),
        model_failure_reason varchar(255),
        prompt_type varchar(255) not null,
        question TEXT not null,
        rationale TEXT,
        student_answer TEXT,
        primary key (id)
    );

    create table hint_safety_checks (
        checked_at timestamp(6) not null,
        id bigserial not null,
        submission_id bigint not null,
        blocked_reasons_json TEXT,
        original_hint TEXT,
        risk_level varchar(255) not null,
        safe_hint TEXT,
        primary key (id)
    );

    create table informatics_knowledge_nodes (
        enabled boolean not null,
        sort_order integer not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        updated_at timestamp(6) not null,
        type varchar(40) not null check (type in ('DOMAIN','CHAPTER','SECTION','TOPIC','KNOWLEDGE_POINT')),
        difficulty varchar(80),
        library_version varchar(80) not null,
        stage varchar(80),
        name varchar(120) not null,
        code varchar(160) not null,
        parent_code varchar(160),
        path varchar(1200) not null,
        aliases varchar(1600),
        description varchar(1600),
        learning_objectives varchar(1600),
        prerequisites varchar(1600),
        typical_problems varchar(1600),
        primary key (id),
        constraint uk_informatics_knowledge_node_code unique (code)
    );

    create table platform_audit_events (
        created_at timestamp(6) with time zone not null,
        id bigserial not null,
        actor_teacher_id uuid,
        event_type varchar(80) not null,
        ip_address varchar(80),
        target_type varchar(80),
        target_id varchar(100),
        detail varchar(1000),
        primary key (id)
    );

    create table problems (
        memory_limit integer not null,
        time_limit integer not null,
        version_no integer not null,
        archived_at timestamp(6),
        created_at timestamp(6),
        id bigserial not null,
        reviewed_at timestamp(6),
        source_problem_id bigint,
        owner_teacher_id uuid,
        reviewed_by uuid,
        series_id uuid not null,
        scope varchar(20) not null check (scope in ('PUBLIC','SHARED','PRIVATE')),
        version_state varchar(30) not null check (version_state in ('DRAFT','REVIEW_PENDING','PUBLISHED','FROZEN','REJECTED','ARCHIVED')),
        review_reason varchar(500),
        ai_prompt_direction TEXT,
        algorithm_strategies TEXT,
        boundary_types TEXT,
        common_mistakes TEXT,
        description TEXT not null,
        difficulty varchar(255) not null check (difficulty in ('EASY','MEDIUM','HARD')),
        knowledge_points TEXT,
        starter_code TEXT,
        title varchar(255) not null,
        primary key (id)
    );

    create table student_ai_feedback_events (
        assignment_id bigint,
        created_at timestamp(6) not null,
        feedback_revision_id bigint,
        id bigserial not null,
        problem_id bigint,
        student_profile_id bigint,
        submission_id bigint not null,
        answer_leak_risk varchar(255),
        event_type varchar(255) not null,
        failure_reason TEXT,
        feedback_source varchar(255),
        feedback_status varchar(255),
        primary key (id)
    );

    create table student_ai_feedback_revisions (
        diagnosis_run_version integer,
        version_number integer not null,
        analysis_id bigint,
        diagnosis_run_id bigint,
        feedback_id bigint,
        generated_at timestamp(6) not null,
        id bigserial not null,
        submission_id bigint not null,
        source varchar(32) not null,
        status varchar(32) not null,
        generation_key varchar(64) not null,
        prompt_version varchar(120),
        provider varchar(120),
        schema_version varchar(120),
        model varchar(180),
        failure_reason TEXT,
        feedback_json TEXT,
        primary key (id),
        constraint uk_feedback_revision_generation unique (submission_id, generation_key)
    );

    create table student_ai_feedbacks (
        generated_at timestamp(6),
        id bigserial not null,
        latest_revision_id bigint,
        submission_id bigint not null unique,
        generation_key varchar(64),
        failure_reason TEXT,
        feedback_json TEXT,
        source varchar(255) not null,
        status varchar(255) not null,
        primary key (id)
    );

    create table student_profiles (
        class_group_id bigint,
        created_at timestamp(6) not null,
        id bigserial not null,
        last_seen_at timestamp(6) not null,
        status varchar(20) not null check (status in ('ACTIVE','INACTIVE','NEEDS_REVIEW')),
        display_name varchar(255) not null,
        identity_key varchar(255) not null,
        note varchar(255),
        student_no varchar(255),
        primary key (id)
    );

    create table student_recommendation_events (
        assignment_id bigint,
        created_at timestamp(6) not null,
        followup_submission_id bigint,
        id bigserial not null,
        problem_id bigint,
        student_profile_id bigint not null,
        event_type varchar(255) not null,
        expected_completion_signal TEXT,
        fallback_action TEXT,
        focus_ability varchar(255),
        focus_tags TEXT,
        followup_fine_grained_tag varchar(255),
        followup_issue_tag varchar(255),
        followup_verdict varchar(255),
        learning_hypothesis TEXT,
        recommendation_token varchar(255) not null,
        risk_level varchar(255),
        strategy varchar(255),
        type varchar(255) not null,
        primary key (id)
    );

    create table student_sessions (
        created_at timestamp(6) with time zone not null,
        expires_at timestamp(6) with time zone not null,
        last_seen_at timestamp(6) with time zone not null,
        revoked_at timestamp(6) with time zone,
        student_profile_id bigint not null,
        id uuid not null,
        token_hash varchar(64) not null unique,
        ip_address varchar(80),
        user_agent varchar(300),
        primary key (id)
    );

    create table submission_analyses (
        generated_at timestamp(6) not null,
        id bigserial not null,
        submission_id bigint not null unique,
        analysis_source varchar(255) not null,
        evidence_json TEXT,
        headline varchar(255) not null,
        report_json TEXT,
        report_markdown TEXT not null,
        scenario varchar(255) not null,
        summary TEXT not null,
        primary key (id)
    );

    create table submission_case_results (
        execution_time float(53),
        is_hidden boolean not null,
        memory_used integer,
        passed boolean not null,
        test_case_number integer not null,
        id bigserial not null,
        submission_id bigint not null,
        actual_output TEXT,
        expected_output TEXT,
        input_snapshot TEXT,
        primary key (id)
    );

    create table submission_diagnosis_facts (
        confidence float(53),
        primary_issue boolean not null,
        analysis_id bigint not null,
        created_at timestamp(6) not null,
        id bigserial not null,
        submission_id bigint not null,
        point_key_version varchar(24),
        display_category varchar(32),
        fact_type varchar(32) not null,
        knowledge_path_status varchar(32) not null,
        library_fit varchar(32),
        point_key_source varchar(32),
        projection_status varchar(32) not null,
        issue_id varchar(120),
        improvement_point_id varchar(160),
        mistake_point_id varchar(160),
        skill_unit_id varchar(160),
        fact_key varchar(180) not null,
        normalized_point_key varchar(220),
        title varchar(500),
        evidence_refs_json TEXT,
        knowledge_path_json TEXT,
        primary key (id),
        constraint uk_submission_diagnosis_fact_key unique (fact_key)
    );

    create table submission_evidence_backfill_batches (
        dry_run boolean not null,
        completed_at timestamp(6),
        cursor_end bigint,
        cursor_start bigint,
        failed_count bigint not null,
        id bigserial not null,
        processed_count bigint not null,
        skipped_count bigint not null,
        started_at timestamp(6) not null,
        success_count bigint not null,
        batch_key varchar(64) not null unique,
        error_json TEXT,
        primary key (id)
    );

    create table submission_issue_transitions (
        effective_attempt boolean not null,
        affected_problem_count bigint not null,
        assignment_id bigint,
        consecutive_effective_count bigint not null,
        created_at timestamp(6) not null,
        current_fact_id bigint,
        current_submission_id bigint not null,
        effective_occurrence_count bigint not null,
        first_seen_submission_id bigint,
        id bigserial not null,
        last_seen_submission_id bigint,
        previous_fact_id bigint,
        previous_submission_id bigint,
        problem_id bigint not null,
        raw_occurrence_count bigint not null,
        student_profile_id bigint not null,
        projection_version varchar(24) not null,
        display_category varchar(32),
        fact_type varchar(32),
        point_key_source varchar(32),
        transition_type varchar(32) not null,
        personal_label varchar(48),
        source_fingerprint varchar(80),
        normalized_point_key varchar(220) not null,
        transition_key varchar(320) not null,
        title varchar(500),
        evidence_submission_ids_json TEXT,
        primary key (id),
        constraint uk_submission_issue_transition_key unique (transition_key)
    );

    create table submissions (
        execution_time float(53),
        language_id integer not null,
        memory_used integer,
        assignment_id bigint,
        id bigserial not null,
        problem_id bigint not null,
        student_profile_id bigint,
        submitted_at timestamp(6),
        compile_output TEXT,
        error_message TEXT,
        language_name varchar(255),
        output TEXT,
        source_code TEXT not null,
        verdict varchar(255) check (verdict in ('PENDING','ACCEPTED','WRONG_ANSWER','TIME_LIMIT_EXCEEDED','MEMORY_LIMIT_EXCEEDED','RUNTIME_ERROR','COMPILATION_ERROR','INTERNAL_ERROR')),
        primary key (id)
    );

    create table teacher_accounts (
        failed_login_count integer not null,
        must_change_password boolean not null,
        created_at timestamp(6) with time zone not null,
        locked_until timestamp(6) with time zone,
        reviewed_at timestamp(6) with time zone,
        updated_at timestamp(6) with time zone not null,
        id uuid not null,
        reviewed_by uuid,
        role varchar(20) not null check (role in ('TEACHER','ADMIN')),
        status varchar(30) not null check (status in ('BOOTSTRAP_REQUIRED','PENDING','ACTIVE','REJECTED','SUSPENDED')),
        username_normalized varchar(80) not null unique,
        password_hash varchar(100) not null,
        display_name varchar(120) not null,
        school_name varchar(200) not null,
        review_reason varchar(500),
        primary key (id)
    );

    create table teacher_ai_quotas (
        additional_units integer not null,
        base_units integer not null,
        reserved_units integer not null,
        used_units integer not null,
        quota_month varchar(7) not null,
        id bigserial not null,
        updated_at timestamp(6) with time zone not null,
        version bigint not null,
        teacher_id uuid not null,
        primary key (id),
        constraint uk_teacher_ai_quota_month unique (teacher_id, quota_month)
    );

    create table teacher_diagnosis_corrections (
        eval_candidate boolean not null,
        assignment_id bigint not null,
        corrected_at timestamp(6) not null,
        id bigserial not null,
        student_profile_id bigint,
        submission_id bigint not null,
        correction_type varchar(40),
        target_issue_id varchar(80),
        target_evidence_ref varchar(240),
        corrected_knowledge_path varchar(800),
        corrected_by varchar(255),
        corrected_fine_grained_tag varchar(255),
        corrected_issue_tag varchar(255) not null,
        original_fine_grained_tag varchar(255),
        original_issue_tag varchar(255),
        teacher_note TEXT,
        primary key (id)
    );

    create table teacher_sessions (
        created_at timestamp(6) with time zone not null,
        expires_at timestamp(6) with time zone not null,
        last_seen_at timestamp(6) with time zone not null,
        revoked_at timestamp(6) with time zone,
        id uuid not null,
        teacher_id uuid not null,
        token_hash varchar(64) not null unique,
        ip_address varchar(80),
        user_agent varchar(300),
        primary key (id)
    );

    create table test_cases (
        is_hidden boolean,
        order_index integer,
        id bigserial not null,
        problem_id bigint not null,
        expected_output TEXT not null,
        input TEXT not null,
        primary key (id)
    );

    create index idx_ai_diagnosis_run_submission 
       on ai_diagnosis_runs (submission_id, version_number);

    create index idx_ai_diagnosis_run_status 
       on ai_diagnosis_runs (status, updated_at);

    create index idx_ai_diagnosis_stage_run 
       on ai_diagnosis_stage_runs (run_id, status);

    create index idx_ai_diagnosis_stage_type 
       on ai_diagnosis_stage_runs (stage_type, status);

    create index idx_ai_usage_teacher_month 
       on ai_usage_events (teacher_id, created_at);

    create index idx_ai_usage_idempotency 
       on ai_usage_events (teacher_id, idempotency_key);

    create index idx_assignment_invites_assignment 
       on assignment_invites (assignment_id);

    create index idx_assignment_tasks_assignment 
       on assignment_tasks (assignment_id);

    create index idx_assignment_tasks_problem 
       on assignment_tasks (problem_id);

    create index idx_class_review_feedback_assignment 
       on class_review_feedback (assignment_id, created_at);

    create index idx_class_review_feedback_key 
       on class_review_feedback (assignment_id, suggestion_key);

    create index idx_coach_prompts_submission 
       on coach_prompts (submission_id, created_at);

    create index idx_coach_prompts_assignment_student 
       on coach_prompts (assignment_id, student_profile_id, created_at);

    create index idx_student_ai_feedback_event_submission 
       on student_ai_feedback_events (submission_id, event_type, created_at);

    create index idx_student_ai_feedback_event_student_problem 
       on student_ai_feedback_events (student_profile_id, problem_id, created_at);

    create index idx_student_ai_feedback_event_assignment 
       on student_ai_feedback_events (assignment_id, created_at);

    create index idx_feedback_revision_submission 
       on student_ai_feedback_revisions (submission_id, version_number);

    create index idx_feedback_revision_status 
       on student_ai_feedback_revisions (status, generated_at);

    create index idx_student_profiles_class 
       on student_profiles (class_group_id);

    create index idx_student_profiles_identity 
       on student_profiles (identity_key);

    create index idx_reco_events_student 
       on student_recommendation_events (student_profile_id, created_at);

    create index idx_reco_events_assignment 
       on student_recommendation_events (assignment_id, created_at);

    create index idx_reco_events_token 
       on student_recommendation_events (recommendation_token);

    create index idx_reco_events_submission 
       on student_recommendation_events (followup_submission_id);

    create index idx_case_results_submission 
       on submission_case_results (submission_id);

    create index idx_case_results_submission_case 
       on submission_case_results (submission_id, test_case_number);

    create index idx_diagnosis_fact_submission 
       on submission_diagnosis_facts (submission_id);

    create index idx_diagnosis_fact_analysis 
       on submission_diagnosis_facts (analysis_id);

    create index idx_diagnosis_fact_path 
       on submission_diagnosis_facts (knowledge_path_status, fact_type);

    create index idx_diagnosis_fact_skill 
       on submission_diagnosis_facts (skill_unit_id);

    create index idx_diagnosis_fact_mistake 
       on submission_diagnosis_facts (mistake_point_id);

    create index idx_diagnosis_fact_normalized_point 
       on submission_diagnosis_facts (normalized_point_key, fact_type);

    create index idx_evidence_backfill_started 
       on submission_evidence_backfill_batches (started_at);

    create index idx_issue_transition_submission 
       on submission_issue_transitions (current_submission_id);

    create index idx_issue_transition_student_point 
       on submission_issue_transitions (student_profile_id, normalized_point_key);

    create index idx_issue_transition_scope 
       on submission_issue_transitions (student_profile_id, assignment_id, problem_id, current_submission_id);

    create index idx_submissions_problem_submitted_at 
       on submissions (problem_id, submitted_at);

    create index idx_submissions_assignment_student_submitted_at 
       on submissions (assignment_id, student_profile_id, submitted_at);

    create index idx_submissions_assignment_student_problem 
       on submissions (assignment_id, student_profile_id, problem_id);

    create index idx_teacher_corrections_assignment 
       on teacher_diagnosis_corrections (assignment_id, corrected_at);

    create index idx_teacher_corrections_submission 
       on teacher_diagnosis_corrections (submission_id, corrected_at);

CREATE TABLE IF NOT EXISTS platform_schema_baseline (
    baseline_id INTEGER PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO platform_schema_baseline (baseline_id, description)
SELECT 1, 'Legacy online judge schema adopted by Flyway'
WHERE NOT EXISTS (SELECT 1 FROM platform_schema_baseline WHERE baseline_id = 1);
