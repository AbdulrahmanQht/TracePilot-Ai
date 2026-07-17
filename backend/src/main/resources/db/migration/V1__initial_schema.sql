create extension if not exists "pgcrypto";

CREATE TYPE oauth_providers AS ENUM (
    'GOOGLE',
    'GITHUB'
);

CREATE TYPE roles AS ENUM (
    'USER',
    'ADMIN'
);

CREATE TYPE audit_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETE',
    'FAILED'
);

CREATE TYPE audit_input_source AS ENUM (
   'PASTED_TEXT',
   'FILE_UPLOAD',
   'CI_LOG',
   'PR_BUNDLE'
);

CREATE TYPE types_of_agents AS ENUM (
   'TRACE_LOOP_EFFICIENCY',
   'BLIND_OUTCOME_VERIFIER',
   'RELIABILITY_TREND'
);

create table users (
	id UUID primary key default gen_random_uuid(),
	email varchar(255) unique not null,
	display_name varchar(100),
	password_hash varchar(255),
	oauth_provider oauth_providers,
	oauth_id varchar(50),
	role roles not null default 'USER',
	is_verified BOOLEAN NOT NULL DEFAULT FALSE,
	audit_count_today INT NOT NULL DEFAULT 0,
	last_audit_date DATE,
	created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verification_token VARCHAR(255),
    verification_token_expires_at TIMESTAMPTZ,
    reset_password_token VARCHAR(255),
    reset_password_token_expires_at TIMESTAMPTZ,
    constraint chk_auth_method check (password_hash is not null or (oauth_provider is not null and oauth_id is not null))
);

create table refresh_tokens (
	id UUID primary key default gen_random_uuid(),
	user_id UUID not null,
	token_hash varchar(255) unique not null,
	expires_at timestamptz not null,
	revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    constraint fk_refresh_user foreign key (user_id) references users(id) on delete cascade
);

create table trace_audits (
	id UUID primary key default gen_random_uuid(),
	user_id UUID not null,
	title VARCHAR(150),
    repo_name VARCHAR(150),
    raw_trace TEXT NOT NULL,
    trace_hash VARCHAR(64) NOT NULL,
    extracted_evidence JSONB,
    withheld_claims JSONB,
    input_source audit_input_source not null default 'PASTED_TEXT',
    agent_tool varchar(30) not null default 'generic',
    status audit_status not null default 'PENDING',
    overall_score int check (overall_score between 0 and 100),
    is_public boolean not null default false,
    share_token varchar(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    suspicious_content boolean not null default false,
    failure_reason VARCHAR(300),
    CONSTRAINT fk_trace_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE cascade,
    constraint chk_public_share check (not is_public or share_token is not null)
);

create table agent_reports (
	id UUID primary key default gen_random_uuid(),
	audit_id UUID NOT NULL,
	agent_type types_of_agents not null,
	findings JSONB not null,
	severity_score int not null check(severity_score between 0 and 100),
	processing_time_ms int,
	created_at timestamptz not null default current_timestamp,
	CONSTRAINT fk_report_audit FOREIGN KEY (audit_id) REFERENCES trace_audits(id) ON DELETE CASCADE,
    CONSTRAINT uq_audit_agent UNIQUE (audit_id, agent_type)
);

create table reliability_history (
	id UUID primary key default gen_random_uuid(),
	user_id UUID not null,
	audit_id UUID NOT NULL,
	repo_name varchar(150),
	agent_tool varchar(30) not null,
	reliability_score int not null check(reliability_score between 0 and 100),
	signal_summary JSONB not null,
	recorded_at timestamptz not null default current_timestamp,
	CONSTRAINT fk_reliability_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reliability_audit FOREIGN KEY (audit_id) REFERENCES trace_audits(id) ON DELETE CASCADE,
    CONSTRAINT uq_reliability_history_audit UNIQUE (audit_id)
);

create unique index uq_users_oauth on users(oauth_provider, oauth_id) where oauth_provider is not null;
create index idx_refresh_tokens_user_id on refresh_tokens(user_id);
create index idx_refresh_tokens_expires_at on refresh_tokens(expires_at) where revoked = false;
create index idx_trace_audits_user_id on trace_audits(user_id);
create index uq_trace_audits_user_hash on trace_audits(user_id, trace_hash);
create unique index idx_trace_audits_share_token on trace_audits(share_token) where share_token is not null;
create index idx_trace_audits_user_created on trace_audits(user_id, created_at desc);
create index idx_agent_reports_audit_id on agent_reports(audit_id);
CREATE INDEX idx_agent_reports_findings ON agent_reports USING GIN (findings);
CREATE INDEX idx_reliability_user_repo_tool ON reliability_history(user_id, repo_name, agent_tool);
CREATE INDEX idx_reliability_recorded_at ON reliability_history(recorded_at);

CREATE OR REPLACE FUNCTION lowercase_email()
RETURNS TRIGGER AS $$
BEGIN
    NEW.email := LOWER(NEW.email);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

create trigger trg_lowercase_email
before insert or update of email
on users
for each row
execute function lowercase_email();


create or replace function set_updated_at() returns trigger as $$
begin
  new.updated_at = current_timestamp;
  return new;
end;
$$ language plpgsql;

create trigger trg_users_updated_at
  before update on users
  for each row execute function set_updated_at();