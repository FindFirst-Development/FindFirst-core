import type { NextApiRequest, NextApiResponse } from "next";
import axios from "axios";
import fs from "fs";
import path from "path";

export default async function handler(
  req: NextApiRequest,
  res: NextApiResponse,
) {
  const { userId } = req.query;

  if (!userId) {
    return res.status(400).json({ error: "Missing userId" });
  }

  try {
    const backendUrl = `${process.env.NEXT_PUBLIC_SERVER_URL}/user/profile-picture?userId=${userId}`;

    const response = await axios.get(backendUrl, {
      responseType: "arraybuffer",
    });
    res.setHeader(
      "Content-Type",
      "image/png",
    );
    res.setHeader("Cache-Control", "public, max-age=3600");
    res.end(Buffer.from(response.data), "binary");
  } catch (err: any) {
    if (err.response?.status === 404) {
      // fallback to default avatar
      const fallbackPath = path.join(process.cwd(), "public", "img_avatar.png");
      const imageBuffer = fs.readFileSync(fallbackPath);
      res.setHeader("Content-Type", "image/png");
      res.setHeader("Cache-Control", "public, max-age=3600");
      res.end(imageBuffer);
      return;
    }
    res
      .status(err.response?.status || 500)
      .json({ error: "Unable to fetch avatar" });
  }
}
