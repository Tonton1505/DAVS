export type SiteRoleType = 'SITE_ADMINISTRATOR' | 'CREATOR' | 'EXPLORER' | 'VIEWER';

// Quan hệ kế thừa: Quản trị viên ⊃ Người sáng tạo ⊃ Người khám phá ⊃ Người xem
export const ROLE_RANK: Record<SiteRoleType, number> = {
  SITE_ADMINISTRATOR: 4,
  CREATOR: 3,
  EXPLORER: 2,
  VIEWER: 1,
};
