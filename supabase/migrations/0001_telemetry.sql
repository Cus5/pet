-- 견심이 2.0 서버 표: 익명 사용 통계, 피드백, 체중 기록(공유에 동의한 사용자만)
--
-- 앱은 공개 키(anon 역할)로 "추가"만 할 수 있습니다. 읽기·수정·삭제 권한은 없고,
-- 서버가 채우는 열(id, created_at)은 앱이 값을 지정할 수 없습니다.
-- install_id 는 앱이 처음 실행될 때 만드는 무작위 UUID 로, 사람이나 기기를 식별하지 않습니다.

-- ── 사용 이벤트: 설치(install_id)·날짜·이벤트 종류마다 하루 한 줄 ─────────────
create table public.usage_events (
  install_id  uuid        not null,
  day         date        not null,
  name        text        not null
    check (name in ('app_open', 'pet_registered', 'meal_logged', 'health_saved', 'post_written', 'feedback_sent')),
  app_version text        not null check (char_length(app_version) between 1 and 30),
  debug       boolean     not null default false,   -- 개발용 빌드에서 보낸 줄 (지표에서 제외)
  created_at  timestamptz not null default now(),
  primary key (install_id, day, name)
);

-- ── 앱 안에서 보낸 의견 ─────────────────────────────────────────────────────
create table public.feedback (
  id          bigint generated always as identity primary key,
  install_id  uuid        not null,
  category    text        not null check (category in ('bug', 'idea', 'praise', 'other')),
  rating      smallint    check (rating between 1 and 5),
  message     text        not null check (char_length(message) between 1 and 2000),
  app_version text        not null check (char_length(app_version) between 1 and 30),
  android_sdk smallint    check (android_sdk between 1 and 200),
  debug       boolean     not null default false,
  created_at  timestamptz not null default now()
);

-- ── 체중 기록: 수정하면 새 줄이 추가되고, 분석할 때 가장 최근 줄을 씁니다 ────────
create table public.weight_records (
  id          bigint generated always as identity primary key,
  install_id  uuid         not null,
  pet_ref     integer      not null check (pet_ref > 0),   -- 기기 안에서의 반려동물 번호 (이름 아님)
  day         date         not null,
  weight_kg   numeric(5,2) not null check (weight_kg > 0 and weight_kg <= 120),
  species     text         not null check (species in ('DOG', 'CAT')),
  breed       text         not null default '' check (char_length(breed) <= 30),
  birth_month date,                                        -- 태어난 달의 1일 (날짜는 보내지 않음)
  sex         text         not null check (sex in ('MALE', 'FEMALE')),
  neutered    boolean      not null,
  app_version text         not null check (char_length(app_version) between 1 and 30),
  debug       boolean      not null default false,
  created_at  timestamptz  not null default now()
);
create index weight_records_pet_idx on public.weight_records (install_id, pet_ref, day, created_at desc);
create index feedback_install_idx on public.feedback (install_id);

-- ── 권한: 추가만 허용 ───────────────────────────────────────────────────────
alter table public.usage_events   enable row level security;
alter table public.feedback       enable row level security;
alter table public.weight_records enable row level security;

revoke all on public.usage_events, public.feedback, public.weight_records from anon, authenticated;
grant insert (install_id, day, name, app_version, debug) on public.usage_events to anon;
grant insert (install_id, category, rating, message, app_version, android_sdk, debug) on public.feedback to anon;
grant insert (install_id, pet_ref, day, weight_kg, species, breed, birth_month, sex, neutered, app_version, debug)
  on public.weight_records to anon;

-- 날짜는 앱(한국 시간)과 서버(UTC)의 차이를 감안해 하루 여유를 둡니다.
create policy anon_insert_usage_events on public.usage_events
  for insert to anon
  with check (day between current_date - 8 and current_date + 1);

create policy anon_insert_feedback on public.feedback
  for insert to anon
  with check (char_length(btrim(message)) > 0);

create policy anon_insert_weight_records on public.weight_records
  for insert to anon
  with check (
    day between date '2015-01-01' and current_date + 1
    and (birth_month is null or birth_month <= current_date + 1)
  );

-- ── 공유를 끈 사용자가 이미 보낸 체중 기록을 지우는 함수 ─────────────────────────
-- install_id 는 그 기기만 아는 추측할 수 없는 값이라, 이 값을 아는 쪽만 자기 기록을 지울 수 있습니다.
-- Supabase 보안 점검(Security Advisor)이 "로그인 없이 실행할 수 있는 SECURITY DEFINER 함수"라고
-- 경고(WARN)를 띄우는데, 로그인이 없는 앱에서 삭제 요청을 받기 위해 의도한 설계입니다.
create function public.delete_my_weight_records(p_install_id uuid)
returns integer
language sql
security definer
set search_path = ''
as $$
  with deleted as (
    delete from public.weight_records where install_id = p_install_id returning 1
  )
  select count(*)::integer from deleted;
$$;
revoke all on function public.delete_my_weight_records(uuid) from public, authenticated;
grant execute on function public.delete_my_weight_records(uuid) to anon;

-- ── 집계용 뷰 (API 로 노출되지 않는 reporting 스키마. Supabase 대시보드 SQL 편집기에서 조회) ──
create schema reporting;

create function reporting.today() returns date
language sql stable set search_path = ''
as $$ select (now() at time zone 'Asia/Seoul')::date $$;

-- 설치별 첫 사용일·마지막 사용일·사용한 날 수
create view reporting.installs with (security_invoker = true) as
select install_id,
       min(day)            as first_day,
       max(day)            as last_day,
       count(distinct day) as active_days
from public.usage_events
where not debug
group by install_id;

-- 같은 날 체중을 고쳐 보낸 경우 가장 최근 값만
create view reporting.weight_latest with (security_invoker = true) as
select distinct on (install_id, pet_ref, day)
       install_id, pet_ref, day, weight_kg, species, breed, birth_month, sex, neutered, app_version, created_at
from public.weight_records
where not debug
order by install_id, pet_ref, day, created_at desc;

-- 지원서에 쓸 숫자 한 줄 요약
create view reporting.summary with (security_invoker = true) as
select
  (select count(*) from reporting.installs)                                              as installs_total,
  (select count(distinct install_id) from public.usage_events
    where not debug and day > reporting.today() - 7)                                     as active_users_7d,
  (select count(distinct install_id) from public.usage_events
    where not debug and day > reporting.today() - 28)                                    as active_users_28d,
  (select count(distinct install_id) from public.usage_events
    where not debug and name in ('meal_logged', 'health_saved'))                         as recording_users_total,
  (select count(*) from public.feedback where not debug)                                 as feedback_count,
  (select count(distinct install_id) from public.feedback where not debug)               as feedback_users,
  (select round(avg(rating), 2) from public.feedback where not debug and rating is not null) as avg_rating,
  (select count(*) from reporting.weight_latest)                                         as weight_points,
  (select count(*) from (select distinct install_id, pet_ref from reporting.weight_latest) p) as pets_with_weights;

-- 4주 유지율: 첫 사용일(0일째) 기준
--   retained_week5_pct : 28~34일째에 하루라도 사용한 비율 (첫 사용 후 34일이 지난 사용자만 계산)
--   still_using_pct    : 28일째 이후 한 번이라도 사용한 비율 (첫 사용 후 28일이 지난 사용자만 계산)
create view reporting.retention_4w with (security_invoker = true) as
with per_install as (
  select i.install_id,
         i.first_day,
         exists (select 1 from public.usage_events e
                  where e.install_id = i.install_id and not e.debug
                    and e.day between i.first_day + 28 and i.first_day + 34) as week5,
         i.last_day >= i.first_day + 28                                     as after_28d
  from reporting.installs i
)
select to_char(first_day, 'YYYY-MM')                                               as cohort_month,
       count(*) filter (where first_day <= reporting.today() - 34)                 as users_34d_old,
       count(*) filter (where first_day <= reporting.today() - 34 and week5)       as retained_week5,
       round(100.0 * count(*) filter (where first_day <= reporting.today() - 34 and week5)
             / nullif(count(*) filter (where first_day <= reporting.today() - 34), 0), 1) as retained_week5_pct,
       count(*) filter (where first_day <= reporting.today() - 28)                 as users_28d_old,
       count(*) filter (where first_day <= reporting.today() - 28 and after_28d)   as still_using,
       round(100.0 * count(*) filter (where first_day <= reporting.today() - 28 and after_28d)
             / nullif(count(*) filter (where first_day <= reporting.today() - 28), 0), 1) as still_using_pct
from per_install
group by rollup (to_char(first_day, 'YYYY-MM'))
order by cohort_month nulls last;   -- 마지막 줄(cohort_month 가 비어 있음)이 전체 합계
