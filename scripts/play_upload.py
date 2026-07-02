#!/usr/bin/env python3
"""Upload the signed release AAB to Google Play (production track) and submit
it for review.

Prereqs (one-time):
  1. Enable the Google Play Android Developer API for the service-account
     project: https://console.developers.google.com/apis/api/androidpublisher.googleapis.com/overview?project=55530757887
  2. The service account must be invited in Play Console → Users & permissions
     with Release-manager rights.

Build first:
  set -a; source keystore/keystore.env; set +a
  export POTLUCK_KEYSTORE="$PWD/keystore/potluck-release.jks"   # keystore.env has a stale path
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundleRelease

Run:
  python3 scripts/play_upload.py [--track production] [--sa /path/to/service-account.json]
"""
import argparse
import os

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

DEFAULT_SA = "/Users/alfredang/projects/tertiary/mobile apps/potluck-489305-77cc4abd9ca7.json"
PKG = "io.potluckhub.app"
AAB = os.path.join(os.path.dirname(__file__), "..", "app/build/outputs/bundle/release/app-release.aab")

RELEASE_NOTES = (
    "• Pay online at checkout — credit/debit card (Stripe), PayPal, or PayNow (HitPay)\n"
    "• Write reviews for chefs and share your favourite chefs & dishes\n"
    "• Verified badges — chefs verified by Potluck with an in-person site visit\n"
    "• Featured chefs, Thai cuisine filter, and polish throughout"
)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--track", default="production")
    ap.add_argument("--sa", default=DEFAULT_SA)
    ap.add_argument("--name", default=None, help="Release name (defaults to versionName from the AAB)")
    args = ap.parse_args()

    creds = service_account.Credentials.from_service_account_file(
        args.sa, scopes=["https://www.googleapis.com/auth/androidpublisher"]
    )
    svc = build("androidpublisher", "v3", credentials=creds)

    edit_id = svc.edits().insert(packageName=PKG, body={}).execute()["id"]
    print("edit:", edit_id)

    bundle = svc.edits().bundles().upload(
        packageName=PKG,
        editId=edit_id,
        media_body=MediaFileUpload(os.path.abspath(AAB), mimetype="application/octet-stream", resumable=True),
    ).execute()
    version_code = bundle["versionCode"]
    print("uploaded versionCode:", version_code)

    svc.edits().tracks().update(
        packageName=PKG,
        editId=edit_id,
        track=args.track,
        body={
            "track": args.track,
            "releases": [
                {
                    "name": args.name or str(version_code),
                    "versionCodes": [str(version_code)],
                    "status": "completed",
                    "releaseNotes": [{"language": "en-US", "text": RELEASE_NOTES}],
                }
            ],
        },
    ).execute()
    print(f"track '{args.track}' updated")

    committed = svc.edits().commit(packageName=PKG, editId=edit_id).execute()
    print("committed edit:", committed["id"], "— release is now in review")


if __name__ == "__main__":
    main()
