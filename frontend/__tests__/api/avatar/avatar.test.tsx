import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import type { NextApiRequest, NextApiResponse } from "next";
import axios from "axios";
import fs from "fs";
import path from "path";
import handler from "pages/api/user/avatar"; // Update with actual path

// Mock dependencies
vi.mock("axios");
vi.mock("fs");
vi.mock("path");

describe("Avatar API Handler", () => {
  let req: Partial<NextApiRequest>;
  let res: Partial<NextApiResponse>;
  let statusMock: ReturnType<typeof vi.fn>;
  let jsonMock: ReturnType<typeof vi.fn>;
  let setHeaderMock: ReturnType<typeof vi.fn>;
  let endMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    // Reset mocks before each test
    vi.clearAllMocks();

    // Setup response mocks
    statusMock = vi.fn().mockReturnThis();
    jsonMock = vi.fn().mockReturnThis();
    setHeaderMock = vi.fn();
    endMock = vi.fn();

    req = {
      query: {},
    };

    res = {
      status: statusMock,
      json: jsonMock,
      setHeader: setHeaderMock,
      end: endMock,
    };

    // Setup environment variable
    process.env.NEXT_PUBLIC_SERVER_URL = "https://api.example.com";
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("should return 400 error when userId is missing", async () => {
    req.query = {};

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(statusMock).toHaveBeenCalledWith(400);
    expect(jsonMock).toHaveBeenCalledWith({ error: "Missing userId" });
  });

  it("should fetch and return profile picture successfully", async () => {
    const mockImageData = Buffer.from("fake-image-data");
    const mockAxiosResponse = {
      data: mockImageData,
      headers: {
        "content-type": "image/png",
      },
    };

    req.query = { userId: "123" };
    vi.mocked(axios.get).mockResolvedValue(mockAxiosResponse);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(axios.get).toHaveBeenCalledWith(
      "https://api.example.com/user/profile-picture?userId=123",
      { responseType: "arraybuffer" },
    );
    expect(setHeaderMock).toHaveBeenCalledWith("Content-Type", "image/png");
    expect(setHeaderMock).toHaveBeenCalledWith(
      "Cache-Control",
      "public, max-age=3600",
    );
    expect(endMock).toHaveBeenCalledWith(expect.any(Buffer), "binary");
  });

  it("should use default content-type when not provided in response", async () => {
    const mockImageData = Buffer.from("fake-image-data");
    const mockAxiosResponse = {
      data: mockImageData,
      headers: {},
    };

    req.query = { userId: "123" };
    vi.mocked(axios.get).mockResolvedValue(mockAxiosResponse);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(setHeaderMock).toHaveBeenCalledWith("Content-Type", "image/png");
  });

  it("should return fallback avatar when backend returns 404", async () => {
    const mockFallbackImage = Buffer.from("fallback-image-data");
    const mockError = {
      response: {
        status: 404,
      },
    };

    req.query = { userId: "123" };
    vi.mocked(axios.get).mockRejectedValue(mockError);
    vi.mocked(path.join).mockReturnValue("/mock/path/to/img_avatar.png");
    vi.mocked(fs.readFileSync).mockReturnValue(mockFallbackImage);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(path.join).toHaveBeenCalledWith(
      process.cwd(),
      "public",
      "img_avatar.png",
    );
    expect(fs.readFileSync).toHaveBeenCalledWith(
      "/mock/path/to/img_avatar.png",
    );
    expect(setHeaderMock).toHaveBeenCalledWith("Content-Type", "image/png");
    expect(setHeaderMock).toHaveBeenCalledWith(
      "Cache-Control",
      "public, max-age=3600",
    );
    expect(endMock).toHaveBeenCalledWith(mockFallbackImage);
  });

  it("should return 500 error when backend fails with non-404 error", async () => {
    const mockError = {
      response: {
        status: 500,
      },
    };

    req.query = { userId: "123" };
    vi.mocked(axios.get).mockRejectedValue(mockError);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(statusMock).toHaveBeenCalledWith(500);
    expect(jsonMock).toHaveBeenCalledWith({ error: "Unable to fetch avatar" });
  });

  it("should return 500 error when no response status is available", async () => {
    const mockError = new Error("Network error");

    req.query = { userId: "123" };
    vi.mocked(axios.get).mockRejectedValue(mockError);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(statusMock).toHaveBeenCalledWith(500);
    expect(jsonMock).toHaveBeenCalledWith({ error: "Unable to fetch avatar" });
  });

  it("should handle userId as array and use first value", async () => {
    const mockImageData = Buffer.from("fake-image-data");
    const mockAxiosResponse = {
      data: mockImageData,
      headers: {
        "content-type": "image/png",
      },
    };

    req.query = { userId: ["123", "456"] };
    vi.mocked(axios.get).mockResolvedValue(mockAxiosResponse);

    await handler(req as NextApiRequest, res as NextApiResponse);

    expect(axios.get).toHaveBeenCalledWith(
      "https://api.example.com/user/profile-picture?userId=123,456",
      { responseType: "arraybuffer" },
    );
  });
});
