export interface CategoryGroup {
  name: string;
  items: string[];
}

export const ALL_CATEGORY_LABEL = '전체';

export const CATEGORY_GROUPS: CategoryGroup[] = [
  { name: '스킨케어', items: ['스킨/토너', '에센스/세럼/앰플', '크림', '로션', '미스트/오일', '스킨케어세트', '스킨케어 디바이스'] },
  { name: '마스크팩', items: ['시트팩', '패드', '페이셜팩', '코팩', '패치'] },
  { name: '클렌징', items: ['클렌징폼/젤', '오일/밤', '워터/밀크', '필링&스크럽', '티슈/패드', '립&아이리무버', '클렌징 디바이스'] },
  { name: '선케어', items: ['선크림', '선스틱', '선쿠션', '선스프레이/선패치', '태닝/애프터선'] },
  { name: '메이크업', items: ['립메이크업', '베이스메이크업', '아이메이크업'] },
  { name: '뷰티소품', items: ['메이크업 툴', '아이래쉬 툴', '페이스 툴', '헤어/바디 툴', '데일리 툴'] },
  { name: '더모코스메틱', items: ['더모_스킨케어', '더모_바디케어', '더모_클렌징', '더모_선케어', '더모_마스크팩'] },
  { name: '네일', items: ['일반네일', '젤네일', '네일팁/스티커', '네일케어/리무버'] },
  { name: '헤어케어', items: ['샴푸/스케일러', '트리트먼트/팩', '두피에센스', '헤어에센스', '염모제/펌', '헤어기기/브러시', '스타일링'] },
  { name: '바디케어', items: ['샤워/입욕', '바스로션/크림', '바스오일/미스트', '제모/왁싱', '데오드란트', '핸드케어', '풋케어', '유아동/임산부'] },
  { name: '향수/디퓨저', items: ['향수', '미니/고체향수', '홈프래그런스'] },
  { name: '건강식품', items: ['비타민', '영양제', '유산균', '슬리밍/이너뷰티'] },
  { name: '푸드', items: ['식단관리/이너뷰티', '과자/초콜릿/디저트', '생수/음료/커피', '간편식/요리', '베이비푸드'] },
];

export function resolveSelectedCategoryGroup(groupName: string, categoryName: string): CategoryGroup | undefined {
  return CATEGORY_GROUPS.find(group => (
    group.name === groupName || group.items.includes(categoryName)
  ));
}

export function getCategoriesParamForGroup(group?: CategoryGroup): string | undefined {
  return group?.items.join(',');
}

export function getGroupNameForCategory(categoryName: string): string | undefined {
  return CATEGORY_GROUPS.find(group => group.items.includes(categoryName))?.name;
}
