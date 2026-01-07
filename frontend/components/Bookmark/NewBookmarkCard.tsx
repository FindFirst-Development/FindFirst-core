import { Field, Form, Formik } from "formik";
import { useState } from "react";
import style from "./bookmarkCard.module.scss";
import api from "@/api/Api";
import Bookmark from "@/types/Bookmarks/Bookmark";
import { useBookmarkDispatch } from "@/contexts/BookmarkContext";
import BookmarkAction from "@/types/Bookmarks/BookmarkAction";
import { useTagsDispatch } from "@/contexts/TagContext";
import TagAction from "@/types/Bookmarks/TagAction";
import Tag from "@/types/Bookmarks/Tag";
import * as Yup from "yup";
import { tlds } from "@type/Bookmarks/TLDS";
import {
  NewBookmarkRequest,
  NewBookmarkForm,
  newcard,
} from "@type/Bookmarks/NewBookmark";
import { ToastContainer, toast } from "react-toastify";
import { AxiosError, AxiosResponse } from "axios";
import { ScrapableNewBookmarkToggle } from "./ScrapableToggle";
import TagInput from "./TagInput";

async function makeNewBookmark(createBmk: Bookmark): Promise<Bookmark> {
  let newBkmkRequest: NewBookmarkRequest = {
    title: createBmk.title,
    url: createBmk.url,
    tagIds: [],
    scrapable: createBmk.scrapable,
  };
  let tagTitles: string[] = createBmk.tags.map((t) => t.title);

  await api.addAllTag(tagTitles).then((response) => {
    let respTags: Tag[] = response.data;
    respTags.forEach((rt) => {
      newBkmkRequest.tagIds.push(rt.id);
    });
  });

  await api
    .addBookmark(newBkmkRequest)
    .then((response: AxiosError<any> | AxiosResponse) => {
      if (response.status === 409) {
        if (response instanceof AxiosError) {
          toast.error(response?.response?.data.error, {
            position: "bottom-right",
            autoClose: 5000,
            hideProgressBar: false,
            closeOnClick: true,
            pauseOnHover: true,
            draggable: true,
            progress: undefined,
            theme: "colored",
          });
        }
        return;
      }
      if (!(response instanceof AxiosError)) {
        // Update the bookmark with the response data
        createBmk.id = response.data.id;
        createBmk.tags = response.data.tags;
        createBmk.screenshotUrl = response.data.screenshotUrl;
        createBmk.scrapable = response.data.scrapable;
        createBmk.title = response.data.title;

        // Show a green success toast for a successful add
        toast.success("Bookmark added successfully!", {
          position: "bottom-right",
          autoClose: 3000,
          hideProgressBar: false,
          closeOnClick: true,
          pauseOnHover: true,
          draggable: true,
          progress: undefined,
          theme: "colored",
        });
      }
    });
  return createBmk;
}

export default function NewBookmarkCard() {
  const [urlInput, setUrlInput] = useState("");
  const [titleInput, setTitleInput] = useState("");
  const [tagInput, setTagInput] = useState("");
  const [isScrapable, setIsScrapable] = useState(true);
  const [strTags, setStrTags] = useState<string[]>([]);
  const bkmkDispatch = useBookmarkDispatch();
  const tagDispatch = useTagsDispatch();

  const urlInputChange = (e: any, setFieldValue: any) => {
    let url: string = e.target.value;
    if (url.length > 4 && !url.startsWith("http")) {
      url = "https://" + url;
    }
    setFieldValue("url", url, true);
    setUrlInput(url);
  };

  const titleInputChange = (e: any, setFieldValue: any) => {
    const title: string = e.target.value;
    setFieldValue("title", title, true);
    setTitleInput(title);
  };

  const handleOnSubmit = async (
    submittedBmk: NewBookmarkForm,
    actions: any
  ) => {
    // Get the last inputted string and all the tags already entered.
    let tags: Tag[] = strTags.map((t) => ({ title: t, id: -1 }));
    if (tagInput) {
      // FIXME
      tags.push({ title: tagInput, id: -1 });
    }

    // Add default "untagged" tag if user has not provided any
    if (!tags.length) {
      tags = [{ title: "untagged", id: -1 }];
    }

    let newBkmk: Bookmark = {
      id: -1,
      title: submittedBmk.title || submittedBmk.url,
      url: submittedBmk.url,
      screenshotUrl: "",
      tags: tags,
      scrapable: isScrapable,
      textHighlight: null,
    };

    actions.resetForm({ newcard });
    clearFormState();
    let retBkmk = await makeNewBookmark(newBkmk);
    // If adding the bookmark was successful, dispatch actions.
    if (retBkmk.id !== -1) {
      retBkmk.tags.forEach((t) => {
        let tAct: TagAction = {
          type: "add",
          id: t.id,
          title: t.title,
          bookmark: retBkmk,
        };
        tagDispatch(tAct);
      });

      let action: BookmarkAction = {
        type: "add",
        bookmarkId: retBkmk.id,
        bookmarks: [retBkmk],
      };
      bkmkDispatch(action);
    }
  };

  const clearFormState = () => {
    setStrTags([]);
    setTagInput("");
    setUrlInput("");
    setTitleInput("");
  };

  const deleteTag = (index: number, setField: any) => {
    const tags = strTags.filter((t, i) => i !== index);
    setField("tagTitles", tags, false);
    setStrTags(tags);
  };

  const onPushTag = (tag: string, setFieldValue: any) => {
    setFieldValue("tagTitles", [...strTags, tag], true);
    setStrTags([...strTags, tag]);
  };

  function stripHttp(fullUrl: string) {
    fullUrl = fullUrl.toLowerCase();
    return fullUrl.startsWith("https://")
      ? fullUrl.replace("https://", "")
      : fullUrl.replace("http://", "");
  }

  function getRootUrl(url: string) {
    return url.split("/")[0];
  }

  const bookmarkSchema = Yup.object().shape({
    url: Yup.string()
      .url()
      .required("Must be a valid URL")
      .min(3)
      .test("The TLD is valid", "The Domain must be valid.", (value) => {
        const url = stripHttp(value);
        const mainUrl = getRootUrl(url);
        const dots = mainUrl.split(".");
        const tld = dots[dots.length - 1];
        return tlds.includes(tld.toUpperCase());
      }),
    title: Yup.string().optional().min(1),
    tagTitles: Yup.array().max(8, "Too many tags. Max 8.").optional(),
  });

  return (
    <div>
      <Formik
        initialValues={newcard}
        onSubmit={handleOnSubmit}
        onReset={clearFormState}
        validateOnChange={true}
        validateOnBlur={true}
        validationSchema={bookmarkSchema}
      >
        {({ isValid, dirty, values, setValues, setFieldValue, errors }) => (
          <Form>
            <div className={`card ${style.newBookmarkCard} ${style.cardBody}`}>
              <div className="card-header">
                Add Bookmark <i className="bi bi-bookmarks-fill"></i>{" "}
              </div>
              <div className="card-body">
                <div className={`card-text ${style.title}`}>
                  <Field
                    className="form-control"
                    id="url"
                    name="url"
                    value={urlInput}
                    onChange={(e: any) => urlInputChange(e, setFieldValue)}
                    placeholder="url: https://findfirst.com/discover"
                    type="text"
                  />
                </div>
                <div style={{ visibility: urlInput ? "visible" : "hidden" }}>
                  <Field
                    className="form-control"
                    id="title"
                    name="title"
                    value={titleInput}
                    onChange={(e: any) => titleInputChange(e, setFieldValue)}
                    placeholder={`title: ${urlInput}`}
                    type="text"
                  />
                  <ScrapableNewBookmarkToggle
                    isScrapable={isScrapable}
                    setScrapable={setIsScrapable}
                    values={values}
                    setValues={setValues}
                  />
                </div>
              </div>
              <div className={`card-footer ${style.cardFooter} `}>
                <TagInput
                  tags={strTags}
                  inputValue={tagInput}
                  setInputValue={setTagInput}
                  onDeleteTag={(index) => deleteTag(index, setFieldValue)}
                  onPushTag={(tag) => onPushTag(tag, setFieldValue)}
                  testIdPrefix="new-bk-"
                ></TagInput>
                <button
                  disabled={!isValid || !dirty}
                  className={`${style.formButton}  ${style.submit}`}
                  type="submit"
                >
                  Submit
                </button>
                <button
                  className={`${style.formButton} ${style.reset}`}
                  type="reset"
                >
                  Reset
                </button>
                {errors.url ? (
                  <div className={style.errorText}>{errors.url}</div>
                ) : null}
                {errors.tagTitles && values.tagTitles ? (
                  <div className={style.errorText}>{errors.tagTitles}</div>
                ) : null}
              </div>
            </div>
          </Form>
        )}
      </Formik>
      <ToastContainer />
    </div>
  );
}
