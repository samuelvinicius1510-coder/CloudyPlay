-- CloudyPlay administrative features.
-- Run this migration in the Supabase SQL Editor before using the dashboard.
create table if not exists public.credit_accounts (
  user_id uuid primary key references auth.users(id) on delete cascade,
  balance bigint not null default 0 check (balance >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.admin_audit_log (
  id bigint generated always as identity primary key,
  actor_user_id uuid not null references auth.users(id),
  action text not null,
  target_user_id uuid,
  amount bigint,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.public_announcements (
  id bigint generated always as identity primary key,
  title text not null check (length(trim(title)) between 1 and 120),
  body text not null check (length(trim(body)) between 1 and 2000),
  active boolean not null default true,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.game_catalog (
  id bigint generated always as identity primary key,
  title text not null check (length(trim(title)) between 1 and 160),
  genre text not null default 'Outros',
  steam_app_id text,
  cover_url text,
  active boolean not null default true,
  created_by uuid not null references auth.users(id),
  created_at timestamptz not null default now()
);

alter table public.credit_accounts enable row level security;
alter table public.admin_audit_log enable row level security;
alter table public.public_announcements enable row level security;
alter table public.game_catalog enable row level security;

drop policy if exists "owners read credit accounts" on public.credit_accounts;
create policy "owners read credit accounts" on public.credit_accounts
 for select to authenticated
 using (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'));

drop policy if exists "owners read audit log" on public.admin_audit_log;
create policy "owners read audit log" on public.admin_audit_log
 for select to authenticated
 using (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'));

drop policy if exists "public read active announcements" on public.public_announcements;
create policy "public read active announcements" on public.public_announcements
 for select to anon, authenticated using (active = true);
drop policy if exists "owners manage announcements" on public.public_announcements;
create policy "owners manage announcements" on public.public_announcements
 for all to authenticated
 using (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'))
 with check (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'));

drop policy if exists "public read active games" on public.game_catalog;
create policy "public read active games" on public.game_catalog
 for select to anon, authenticated using (active = true);
drop policy if exists "owners manage games" on public.game_catalog;
create policy "owners manage games" on public.game_catalog
 for all to authenticated
 using (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'))
 with check (exists (select 1 from public.admin_profiles p where p.user_id = (select auth.uid()) and p.role = 'owner'));

grant select on public.credit_accounts, public.admin_audit_log to authenticated;
grant select on public.public_announcements, public.game_catalog to anon, authenticated;
grant insert, update, delete on public.public_announcements, public.game_catalog to authenticated;
grant usage, select on all sequences in schema public to authenticated;

create or replace function public.admin_adjust_credits(p_target_user_id uuid, p_delta bigint, p_reason text)
returns bigint
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_balance bigint;
begin
  if auth.uid() is null or not exists (
    select 1 from public.admin_profiles p
    where p.user_id = auth.uid() and p.role = 'owner'
  ) then
    raise exception 'Not authorized';
  end if;
  if p_target_user_id is null or p_delta = 0 then
    raise exception 'Target user and non-zero delta are required';
  end if;
  if abs(p_delta) > 1000000000 then
    raise exception 'Adjustment is too large';
  end if;

  insert into public.credit_accounts(user_id, balance)
  values (p_target_user_id, 0)
  on conflict (user_id) do nothing;

  update public.credit_accounts
     set balance = balance + p_delta, updated_at = now()
   where user_id = p_target_user_id and balance + p_delta >= 0
   returning balance into v_balance;

  if v_balance is null then
    raise exception 'Insufficient credits for this removal';
  end if;

  insert into public.admin_audit_log(actor_user_id, action, target_user_id, amount, details)
  values (auth.uid(), case when p_delta > 0 then 'credits_added' else 'credits_removed' end,
          p_target_user_id, p_delta, jsonb_build_object('reason', left(coalesce(p_reason, ''), 500)));

  return v_balance;
end;
$$;

revoke all on function public.admin_adjust_credits(uuid, bigint, text) from public, anon;
grant execute on function public.admin_adjust_credits(uuid, bigint, text) to authenticated;
