import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ChartCanvas } from "../components/ChartCanvas";

function createMock2dContext(): CanvasRenderingContext2D {
  return {
    fillStyle: "",
    strokeStyle: "",
    font: "",
    lineWidth: 1,
    textAlign: "left",
    textBaseline: "alphabetic",
    save: vi.fn(),
    restore: vi.fn(),
    setTransform: vi.fn(),
    fillRect: vi.fn(),
    beginPath: vi.fn(),
    moveTo: vi.fn(),
    lineTo: vi.fn(),
    stroke: vi.fn(),
    fillText: vi.fn(),
    measureText: () => ({ width: 40 }) as TextMetrics,
  } as unknown as CanvasRenderingContext2D;
}

describe("<ChartCanvas /> and <Tooltip />", () => {
  it("renders canvas and shows tooltip on bar hover", () => {
    vi.spyOn(HTMLCanvasElement.prototype, "getContext").mockImplementation(() =>
      createMock2dContext(),
    );
    vi.spyOn(HTMLCanvasElement.prototype, "getBoundingClientRect").mockReturnValue({
      left: 0,
      top: 0,
      right: 640,
      bottom: 360,
      width: 640,
      height: 360,
      x: 0,
      y: 0,
      toJSON: () => ({}),
    });

    const data = [
      { month: "Tháng 1", sales: 420, region: "Miền Bắc" },
      { month: "Tháng 2", sales: 850, region: "Miền Nam" },
    ];

    render(
      <ChartCanvas
        data={data}
        encoding={{ x: "month", y: "sales", color: "region" }}
        ariaLabel="Biểu đồ doanh số"
      />,
    );

    const canvas = screen.getByRole("img", { name: "Biểu đồ doanh số" });
    expect(canvas).toBeInTheDocument();

    // Hover near the first bar inside plot area (left margin = 60, plot width = 560)
    fireEvent.mouseMove(canvas, { clientX: 160, clientY: 250 });

    const tooltip = screen.getByRole("tooltip");
    expect(tooltip).toHaveTextContent("Tháng 1");
    expect(tooltip).toHaveTextContent("420");
    expect(tooltip).toHaveTextContent("Miền Bắc");

    fireEvent.mouseLeave(canvas);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
  });
});
