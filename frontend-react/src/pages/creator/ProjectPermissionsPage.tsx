import { useNavigate, useParams } from "react-router";
import { Card, PageHeader } from "../../components/ui";
import { PermissionsPanelBody } from "../../components/permissions/PermissionsDialog";

export default function ProjectPermissionsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  if (!id) {
    return null;
  }

  return (
    <>
      <PageHeader title="Quyền dự án" eyebrow="Dự án › Thiết lập quyền" />
      <Card className="mx-auto max-w-4xl">
        <PermissionsPanelBody
          resourceType="project"
          resourceId={id}
          onClose={() => navigate("/projects")}
        />
      </Card>
    </>
  );
}
