package com.petstore.user.document;

import java.io.Serializable;

/**
 * Embedded document representing user preferences (category, language, banner display).
 */
public class UserProfileDocument implements Serializable {

  private static final long serialVersionUID = 1L;

  private String favoriteCategory = "FISH";
  private String preferredLanguage = "en_US";
  private boolean bannerPreference = true;
  private boolean myListPreference = true;

  public UserProfileDocument() {}

  public UserProfileDocument(
      String favoriteCategory,
      String preferredLanguage,
      boolean bannerPreference,
      boolean myListPreference) {
    this.favoriteCategory = favoriteCategory;
    this.preferredLanguage = preferredLanguage;
    this.bannerPreference = bannerPreference;
    this.myListPreference = myListPreference;
  }

  public String getFavoriteCategory() {
    return favoriteCategory;
  }

  public void setFavoriteCategory(String favoriteCategory) {
    this.favoriteCategory = favoriteCategory;
  }

  public String getPreferredLanguage() {
    return preferredLanguage;
  }

  public void setPreferredLanguage(String preferredLanguage) {
    this.preferredLanguage = preferredLanguage;
  }

  public boolean isBannerPreference() {
    return bannerPreference;
  }

  public void setBannerPreference(boolean bannerPreference) {
    this.bannerPreference = bannerPreference;
  }

  public boolean isMyListPreference() {
    return myListPreference;
  }

  public void setMyListPreference(boolean myListPreference) {
    this.myListPreference = myListPreference;
  }
}
