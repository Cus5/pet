-- 같은 날 체중을 고쳐 보낸 줄들이 같은 시각(created_at)으로 들어와도
-- 항상 나중에 들어온 줄(id 가 큰 줄)을 고르도록 순서를 확실히 합니다.
create or replace view reporting.weight_latest with (security_invoker = true) as
select distinct on (install_id, pet_ref, day)
       install_id, pet_ref, day, weight_kg, species, breed, birth_month, sex, neutered, app_version, created_at
from public.weight_records
where not debug
order by install_id, pet_ref, day, created_at desc, id desc;
