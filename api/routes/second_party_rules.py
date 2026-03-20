from __future__ import annotations

from typing import Any, Dict, List, Optional

from fastapi import APIRouter

from api.schemas import (
    SecondPartyRuleCreateRequest,
    SecondPartyRuleListResponse,
    SecondPartyRuleResponse,
    SecondPartyRuleUpdateRequest,
)
from storage.sqlite.business.business_db import BusinessDBConfig, get_business_db
from storage.sqlite.business.second_party_rules_repo import SecondPartyRulesRepo

router = APIRouter(prefix="/api", tags=["second-party-rules"])


def _repo() -> SecondPartyRulesRepo:
    # business.db 默认路径由 BusinessDBConfig 决定
    db = get_business_db(BusinessDBConfig())
    return SecondPartyRulesRepo(db=db)


@router.get("/second-party-rules", response_model=SecondPartyRuleListResponse)
def list_second_party_rules() -> SecondPartyRuleListResponse:
    rules = _repo().list_rules()
    return SecondPartyRuleListResponse(ok=True, items=rules)


@router.post("/second-party-rules", response_model=SecondPartyRuleResponse)
def create_second_party_rule(req: SecondPartyRuleCreateRequest) -> SecondPartyRuleResponse:
    rule = _repo().create_rule(
        enabled=req.enabled,
        sort_order=req.sort_order,
        group_id_regex=req.group_id_regex,
        artifact_id_regex=req.artifact_id_regex,
        target_project_name=req.target_project_name,
    )
    return SecondPartyRuleResponse(ok=True, item=rule)


@router.put("/second-party-rules/{rule_id}", response_model=SecondPartyRuleResponse)
def update_second_party_rule(rule_id: int, req: SecondPartyRuleUpdateRequest) -> SecondPartyRuleResponse:
    rule = _repo().update_rule(
        rule_id=rule_id,
        enabled=req.enabled,
        sort_order=req.sort_order,
        group_id_regex=req.group_id_regex,
        artifact_id_regex=req.artifact_id_regex,
        target_project_name=req.target_project_name,
    )
    if rule is None:
        return SecondPartyRuleResponse(ok=False, message=f"rule_id={rule_id} 不存在", item=None)
    return SecondPartyRuleResponse(ok=True, item=rule)


@router.delete("/second-party-rules/{rule_id}")
def delete_second_party_rule(rule_id: int) -> Dict[str, Any]:
    ok = _repo().delete_rule(rule_id)
    if not ok:
        return {"ok": False, "message": f"rule_id={rule_id} 不存在", "deleted_id": rule_id}
    return {"ok": True, "deleted_id": rule_id}

